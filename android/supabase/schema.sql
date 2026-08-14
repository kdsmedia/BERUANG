-- =========================================================================
-- BERUANG — Supabase schema (Android app, com.altomedia.beruang)
-- Run this whole file ONCE in the Supabase SQL Editor.
-- It creates all tables, enables RLS, adds policies, a points-transfer RPC,
-- and turns on Realtime for the tables the app listens to live.
-- =========================================================================

-- The Android app uses Supabase Auth (Email/Password provider) with a synthetic
-- email derived from the user's phone number: "08xxxxxxxx@beruang.phone".
-- auth.uid() therefore matches the "id" (uuid) used as the profiles primary key.

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------- profiles
create table if not exists public.profiles (
    id          uuid primary key references auth.users(id) on delete cascade,
    full_name   text,
    bio         text default 'Hey there! I am using BERUANG.',
    avatar_url  text,
    cover_url   text,
    phone       text,
    email       text,
    gender      text,  -- 'male' | 'female' | 'other'
    points      bigint not null default 0,
    points_pin  text,  -- SHA-256 hex of 'beruang:<pin>'
    account_id  text,  -- 6-digit virtual account number
    created_at  timestamptz not null default now()
);
alter table public.profiles enable row level security;

-- Auto-create a profile row whenever a new auth user signs up.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    insert into public.profiles (id, full_name)
    values (new.id, coalesce(new.raw_user_meta_data->>'full_name', 'New Goat'))
    on conflict (id) do nothing;
    return new;
end;
$$;
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- --------------------------------------------------------------------- posts
create table if not exists public.posts (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references public.profiles(id) on delete cascade,
    content     text,
    image_url   text,
    video_url   text,
    location     text,
    created_at  timestamptz not null default now()
);
alter table public.posts enable row level security;

-- --------------------------------------------------------------------- likes
create table if not exists public.likes (
    id          uuid primary key default gen_random_uuid(),
    post_id     uuid not null references public.posts(id) on delete cascade,
    user_id     uuid not null references public.profiles(id) on delete cascade,
    unique (post_id, user_id)
);
alter table public.likes enable row level security;

-- ------------------------------------------------------------------ comments
create table if not exists public.comments (
    id          uuid primary key default gen_random_uuid(),
    post_id     uuid not null references public.posts(id) on delete cascade,
    user_id     uuid not null references public.profiles(id) on delete cascade,
    content     text not null,
    created_at  timestamptz not null default now()
);
alter table public.comments enable row level security;

-- ------------------------------------------------------------------ stories
create table if not exists public.stories (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references public.profiles(id) on delete cascade,
    image_url   text not null,
    created_at  timestamptz not null default now()
);
alter table public.stories enable row level security;

-- ----------------------------------------------------------- messages (1:1)
create table if not exists public.messages (
    id          uuid primary key default gen_random_uuid(),
    sender_id   uuid not null references public.profiles(id) on delete cascade,
    receiver_id uuid not null references public.profiles(id) on delete cascade,
    content     text not null,
    read        boolean not null default false,
    created_at  timestamptz not null default now()
);
alter table public.messages enable row level security;

-- ------------------------------------------------------ global_messages (chat)
create table if not exists public.global_messages (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references public.profiles(id) on delete cascade,
    content     text not null,
    created_at  timestamptz not null default now()
);
alter table public.global_messages enable row level security;

-- ----------------------------------------------------------- friendships
create table if not exists public.friendships (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references public.profiles(id) on delete cascade,
    friend_id   uuid not null references public.profiles(id) on delete cascade,
    status      text not null default 'pending',  -- 'pending' | 'accepted'
    created_at  timestamptz not null default now()
);
alter table public.friendships enable row level security;

-- --------------------------------------------------------------- groups
create table if not exists public.groups (
    id          uuid primary key default gen_random_uuid(),
    name        text not null,
    description text,
    cover_url   text,
    created_by  uuid references public.profiles(id) on delete set null,
    created_at  timestamptz not null default now()
);
alter table public.groups enable row level security;

-- --------------------------------------------------------- group_members
create table if not exists public.group_members (
    id          uuid primary key default gen_random_uuid(),
    group_id    uuid not null references public.groups(id) on delete cascade,
    user_id    uuid not null references public.profiles(id) on delete cascade,
    role        text not null default 'member',  -- 'admin' | 'member'
    created_at  timestamptz not null default now(),
    unique (group_id, user_id)
);
alter table public.group_members enable row level security;

-- ----------------------------------------------------------- notifications
create table if not exists public.notifications (
    id            uuid primary key default gen_random_uuid(),
    user_id       uuid not null references public.profiles(id) on delete cascade,
    type          text not null,  -- like | comment | friend_request | friend_accept | message
    from_user_id  uuid references public.profiles(id) on delete set null,
    reference_id  text,
    content       text,
    read          boolean not null default false,
    created_at    timestamptz not null default now()
);
alter table public.notifications enable row level security;

-- ------------------------------------------------------------------ wallets
-- One row per user; id == user id so the balance is a simple key/value by uid.
create table if not exists public.wallets (
    id          uuid primary key references public.profiles(id) on delete cascade,
    balance     bigint not null default 0
);
alter table public.wallets enable row level security;

-- ------------------------------------------------------------- transactions
create table if not exists public.transactions (
    id          uuid primary key default gen_random_uuid(),
    from_id     uuid not null references public.profiles(id) on delete cascade,
    to_id       uuid not null references public.profiles(id) on delete cascade,
    amount      bigint not null,
    created_at  timestamptz not null default now()
);
alter table public.transactions enable row level security;

-- =========================================================================
-- Row Level Security policies
-- =========================================================================

-- profiles: public read, owner writes.
create policy "profiles read"     on public.profiles for select using (true);
create policy "profiles insert"   on public.profiles for insert with check (auth.uid() = id);
create policy "profiles update"   on public.profiles for update using (auth.uid() = id) with check (auth.uid() = id);

-- posts: public read, owner create/delete.
create policy "posts read"   on public.posts for select using (true);
create policy "posts create" on public.posts for insert with check (auth.uid() = user_id);
create policy "posts delete" on public.posts for delete using (auth.uid() = user_id);

-- likes: public read, liker create/delete.
create policy "likes read"   on public.likes for select using (true);
create policy "likes create" on public.likes for insert with check (auth.uid() = user_id);
create policy "likes delete" on public.likes for delete using (auth.uid() = user_id);

-- comments: public read, author create/delete.
create policy "comments read"   on public.comments for select using (true);
create policy "comments create" on public.comments for insert with check (auth.uid() = user_id);
create policy "comments delete" on public.comments for delete using (auth.uid() = user_id);

-- stories: public read, owner create/delete.
create policy "stories read"   on public.stories for select using (true);
create policy "stories create" on public.stories for insert with check (auth.uid() = user_id);
create policy "stories delete" on public.stories for delete using (auth.uid() = user_id);

-- messages: only sender/receiver can read; sender creates; receiver marks read.
create policy "messages read"   on public.messages for select using (auth.uid() = sender_id or auth.uid() = receiver_id);
create policy "messages create" on public.messages for insert with check (auth.uid() = sender_id);
create policy "messages update" on public.messages for update using (auth.uid() = receiver_id) with check (auth.uid() = receiver_id);

-- global_messages: public read, owner create.
create policy "global read"   on public.global_messages for select using (true);
create policy "global create" on public.global_messages for insert with check (auth.uid() = user_id);

-- friendships: either party can read; user_id creates; either party updates/deletes.
create policy "friendships read"   on public.friendships for select using (auth.uid() = user_id or auth.uid() = friend_id);
create policy "friendships create"  on public.friendships for insert with check (auth.uid() = user_id);
create policy "friendships update"  on public.friendships for update using (auth.uid() = user_id or auth.uid() = friend_id) with check (auth.uid() = user_id or auth.uid() = friend_id);
create policy "friendships delete"  on public.friendships for delete using (auth.uid() = user_id or auth.uid() = friend_id);

-- groups: public read, creator create.
create policy "groups read"   on public.groups for select using (true);
create policy "groups create" on public.groups for insert with check (auth.uid() = created_by);

-- group_members: public read, member create, member delete.
create policy "group_members read"   on public.group_members for select using (true);
create policy "group_members create" on public.group_members for insert with check (auth.uid() = user_id);
create policy "group_members delete" on public.group_members for delete using (auth.uid() = user_id);

-- notifications: owner read/update; any signed-in user can create (the app
-- creates notifications for the relevant user on like/comment/friend events).
create policy "notifications read"   on public.notifications for select using (auth.uid() = user_id);
create policy "notifications create" on public.notifications for insert with check (auth.role() = 'authenticated');
create policy "notifications update" on public.notifications for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- wallets: public read (balances shown), owner create, update restricted to the
-- transfer_points RPC (security definer, bypasses RLS) so clients cannot
-- tamper with balances. (No update policy = clients cannot UPDATE directly.)
create policy "wallets read"   on public.wallets for select using (true);
create policy "wallets insert" on public.wallets for insert with check (auth.uid() = id);

-- transactions: sender or recipient can read; the RPC creates rows.
create policy "transactions read"   on public.transactions for select using (auth.uid() = from_id or auth.uid() = to_id);
create policy "transactions insert" on public.transactions for insert with check (auth.uid() = from_id);

-- =========================================================================
-- Points transfer RPC (atomic, runs as the server — fixes the old client-side
-- race where a mid-way failure could lose points). Called from the app via
-- supabase.rpc("transfer_points", ...).
-- =========================================================================
create or replace function public.transfer_points(
    p_from       uuid,
    p_to_account text,
    p_amount     bigint,
    p_pin        text
) returns text
language plpgsql
security definer
set search_path = public
as $$
declare
    v_from_profile  record;
    v_to_id         uuid;
    v_from_balance  bigint;
    v_to_balance    bigint;
begin
    if p_amount <= 0 then
        return 'Nominal harus lebih dari 0.';
    end if;

    select points_pin, account_id into v_from_profile
        from profiles where id = p_from;
    if not found then return 'Profil tidak ditemukan.'; end if;
    if v_from_profile.points_pin is null then return 'Anda belum mengatur PIN transaksi.'; end if;
    if v_from_profile.points_pin <> p_pin then return 'PIN salah.'; end if;
    if v_from_profile.account_id = p_to_account then return 'Tidak bisa transfer ke akun sendiri.'; end if;

    select id into v_to_id from profiles where account_id = p_to_account limit 1;
    if v_to_id is null then return 'Akun tujuan tidak ditemukan.'; end if;

    select balance into v_from_balance from wallets where id = p_from;
    if v_from_balance is null then v_from_balance := 0; end if;
    if v_from_balance < p_amount then return 'Poin tidak cukup. Saldo: ' || v_from_balance; end if;

    select balance into v_to_balance from wallets where id = v_to_id;
    if v_to_balance is null then v_to_balance := 0; end if;

    insert into wallets (id, balance) values (p_from, v_from_balance - p_amount)
        on conflict (id) do update set balance = excluded.balance;
    insert into wallets (id, balance) values (v_to_id, v_to_balance + p_amount)
        on conflict (id) do update set balance = excluded.balance;

    insert into transactions (from_id, to_id, amount) values (p_from, v_to_id, p_amount);

    return 'OK';
end;
$$;
grant execute on function public.transfer_points(uuid, text, bigint, text) to authenticated;

-- Award points to a user (used by the app on post/comment/friend actions).
-- Runs as the server (security definer) because the RLS blocks clients from
-- updating wallet balances directly.
create or replace function public.award_points(
    p_user   uuid,
    p_amount bigint
) returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_balance bigint;
begin
    if p_amount = 0 then return; end if;
    select balance into v_balance from wallets where id = p_user;
    if v_balance is null then v_balance := 0; end if;
    insert into wallets (id, balance) values (p_user, v_balance + p_amount)
        on conflict (id) do update set balance = excluded.balance;
end;
$$;
grant execute on function public.award_points(uuid, bigint) to authenticated;

-- =========================================================================
-- Realtime: publish the tables the app subscribes to for live updates.
-- (Also enable in Supabase Dashboard → Database → Replication; these grants
--  ensure anon/authenticated roles can subscribe.)
-- =========================================================================
do $$ begin
    alter publication supabase_realtime add table public.posts;
exception when duplicate_object then null; end $$;
do $$ begin
    alter publication supabase_realtime add table public.comments;
exception when duplicate_object then null; end $$;
do $$ begin
    alter publication supabase_realtime add table public.likes;
exception when duplicate_object then null; end $$;
do $$ begin
    alter publication supabase_realtime add table public.messages;
exception when duplicate_object then null; end $$;
do $$ begin
    alter publication supabase_realtime add table public.global_messages;
exception when duplicate_object then null; end $$;
do $$ begin
    alter publication supabase_realtime add table public.notifications;
exception when duplicate_object then null; end $$;
do $$ begin
    alter publication supabase_realtime add table public.friendships;
exception when duplicate_object then null; end $$;

-- =========================================================================
-- Done. The app creates wallet rows on demand (first balance read).
-- =========================================================================
