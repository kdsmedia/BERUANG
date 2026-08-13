-- =====================================================================
-- BERUANG — Supabase setup script
-- ---------------------------------------------------------------------
-- RUN THIS ONCE in the Supabase SQL Editor (Dashboard > SQL Editor)
-- before opening index.html. It creates the tables, enables RLS, sets
-- up the storage buckets, and installs the auto-profile trigger.
--
-- After running, paste your project URL + anon/publishable key into the
-- two constants at the top of index.html.
-- =====================================================================

-- ==========================================
-- 1. CREATE TABLES
-- ==========================================

-- Profiles (Linked to Supabase Auth)
CREATE TABLE IF NOT EXISTS profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  full_name TEXT,
  bio TEXT DEFAULT 'Hey there! I am using BERUANG.',
  avatar_url TEXT,
  cover_url TEXT,
  phone TEXT,
  email TEXT,
  gender TEXT CHECK (gender IS NULL OR gender IN ('male','female','other')),
  points BIGINT NOT NULL DEFAULT 0,
  account_id TEXT UNIQUE,
  points_pin TEXT, -- SHA-256 hex digest of the 4-digit transaction PIN
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Posts (Includes Video & Location)
CREATE TABLE IF NOT EXISTS posts (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  content TEXT,
  image_url TEXT,
  video_url TEXT,
  location TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Likes
CREATE TABLE IF NOT EXISTS likes (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  post_id UUID REFERENCES posts(id) ON DELETE CASCADE NOT NULL,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  UNIQUE(post_id, user_id)
);

-- Comments
CREATE TABLE IF NOT EXISTS comments (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  post_id UUID REFERENCES posts(id) ON DELETE CASCADE NOT NULL,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Stories
CREATE TABLE IF NOT EXISTS stories (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  image_url TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Private Messages
CREATE TABLE IF NOT EXISTS messages (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  sender_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  receiver_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  content TEXT NOT NULL,
  read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Global Chat Messages
CREATE TABLE IF NOT EXISTS global_messages (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  content TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Friendships
CREATE TABLE IF NOT EXISTS friendships (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  friend_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'accepted')),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, friend_id)
);

-- Groups
CREATE TABLE IF NOT EXISTS groups (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  cover_url TEXT,
  created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Group Members
CREATE TABLE IF NOT EXISTS group_members (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  group_id UUID REFERENCES groups(id) ON DELETE CASCADE NOT NULL,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  role TEXT DEFAULT 'member' CHECK (role IN ('admin', 'member')),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(group_id, user_id)
);

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  type TEXT,
  from_user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  reference_id UUID,
  content TEXT,
  read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Wallets (one row per user; authoritative points balance for transfers)
CREATE TABLE IF NOT EXISTS wallets (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  balance BIGINT NOT NULL DEFAULT 0
);

-- Transactions (points transfers between users)
CREATE TABLE IF NOT EXISTS transactions (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  from_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  to_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  amount BIGINT NOT NULL CHECK (amount > 0),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Point events (audit log for activity rewards: post/comment/friend)
CREATE TABLE IF NOT EXISTS point_events (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  reason TEXT NOT NULL, -- 'post' | 'comment' | 'friend'
  amount BIGINT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);




-- ==========================================
-- 2. ENABLE ROW LEVEL SECURITY (RLS)
-- ==========================================
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE stories ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE global_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE friendships ENABLE ROW LEVEL SECURITY;
ALTER TABLE groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE point_events ENABLE ROW LEVEL SECURITY;




-- ==========================================
-- 3. CREATE RLS POLICIES
-- ==========================================

-- PROFILES
CREATE POLICY "Public profiles are viewable by everyone" ON profiles FOR SELECT USING (true);
CREATE POLICY "Users can insert their own profile" ON profiles FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);

-- POSTS
CREATE POLICY "Posts are viewable by everyone" ON posts FOR SELECT USING (true);
CREATE POLICY "Users can insert posts" ON posts FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can delete own posts" ON posts FOR DELETE USING (auth.uid() = user_id);

-- LIKES
CREATE POLICY "Likes are viewable by everyone" ON likes FOR SELECT USING (true);
CREATE POLICY "Users can like posts" ON likes FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can unlike posts" ON likes FOR DELETE USING (auth.uid() = user_id);

-- COMMENTS
CREATE POLICY "Comments are viewable by everyone" ON comments FOR SELECT USING (true);
CREATE POLICY "Users can insert comments" ON comments FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can delete own comments" ON comments FOR DELETE USING (auth.uid() = user_id);

-- STORIES
CREATE POLICY "Stories are viewable by everyone" ON stories FOR SELECT USING (true);
CREATE POLICY "Users can insert stories" ON stories FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can delete own stories" ON stories FOR DELETE USING (auth.uid() = user_id);

-- MESSAGES (Private Chat)
CREATE POLICY "Users can view their own messages" ON messages FOR SELECT USING (auth.uid() = sender_id OR auth.uid() = receiver_id);
CREATE POLICY "Users can send messages" ON messages FOR INSERT WITH CHECK (auth.uid() = sender_id);
CREATE POLICY "Users can update read status of received messages" ON messages FOR UPDATE USING (auth.uid() = receiver_id);

-- GLOBAL MESSAGES
CREATE POLICY "Global messages viewable by everyone" ON global_messages FOR SELECT USING (true);
CREATE POLICY "Authenticated users can send global messages" ON global_messages FOR INSERT WITH CHECK (auth.uid() = user_id);

-- FRIENDSHIPS
CREATE POLICY "Users can view their friendships" ON friendships FOR SELECT USING (auth.uid() = user_id OR auth.uid() = friend_id);
CREATE POLICY "Users can send friend requests" ON friendships FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update friendships they are part of" ON friendships FOR UPDATE USING (auth.uid() = user_id OR auth.uid() = friend_id);
CREATE POLICY "Users can delete friendships they are part of" ON friendships FOR DELETE USING (auth.uid() = user_id OR auth.uid() = friend_id);

-- GROUPS
CREATE POLICY "Groups are viewable by everyone" ON groups FOR SELECT USING (true);
CREATE POLICY "Users can create groups" ON groups FOR INSERT WITH CHECK (auth.uid() = created_by);

-- GROUP MEMBERS
CREATE POLICY "Group members viewable by everyone" ON group_members FOR SELECT USING (true);
CREATE POLICY "Users can join groups" ON group_members FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can leave groups" ON group_members FOR DELETE USING (auth.uid() = user_id);

-- NOTIFICATIONS
CREATE POLICY "Users can view their own notifications" ON notifications FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "System can create notifications" ON notifications FOR INSERT WITH CHECK (true); -- Allows triggers or other users to insert
CREATE POLICY "Users can update their own notifications" ON notifications FOR UPDATE USING (auth.uid() = user_id);

-- WALLETS (owner read only; all writes go through SECURITY DEFINER functions)
CREATE POLICY "Users can read own wallet" ON wallets FOR SELECT USING (auth.uid() = user_id);

-- TRANSACTIONS (sender & recipient can read their own transfers)
CREATE POLICY "Users can view their transactions" ON transactions FOR SELECT USING (auth.uid() = from_id OR auth.uid() = to_id);

-- POINT EVENTS (owner can read their own reward history)
CREATE POLICY "Users can view their own point events" ON point_events FOR SELECT USING (auth.uid() = user_id);




-- ==========================================
-- 4. STORAGE BUCKETS & POLICIES
-- ==========================================

-- Create Storage Buckets for Posts and Avatars
INSERT INTO storage.buckets (id, name, public) VALUES ('posts', 'posts', true) ON CONFLICT DO NOTHING;
INSERT INTO storage.buckets (id, name, public) VALUES ('avatars', 'avatars', true) ON CONFLICT DO NOTHING;

-- Storage Policies: Allow users to upload files into a folder named by their user_id
CREATE POLICY "Users can upload posts images" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'posts' AND (storage.foldername(name))[1] = auth.uid()::text);
CREATE POLICY "Users can upload avatar images" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'avatars' AND (storage.foldername(name))[1] = auth.uid()::text);

-- Allow anyone to view images (since buckets are public, this is technically optional but good practice)
CREATE POLICY "Public read access for posts" ON storage.objects FOR SELECT USING (bucket_id = 'posts');
CREATE POLICY "Public read access for avatars" ON storage.objects FOR SELECT USING (bucket_id = 'avatars');

-- Allow users to delete their own uploads
CREATE POLICY "Users can delete own posts" ON storage.objects FOR DELETE USING (bucket_id = 'posts' AND (storage.foldername(name))[1] = auth.uid()::text);
CREATE POLICY "Users can delete own avatars" ON storage.objects FOR DELETE USING (bucket_id = 'avatars' AND (storage.foldername(name))[1] = auth.uid()::text);




-- ==========================================
-- 5. EXTENSIONS & POINTS FUNCTIONS
-- ==========================================

-- pgcrypto provides digest() for SHA-256 hashing of the transaction PIN.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Hash a 4-digit PIN exactly like the Android app (SHA-256 of "beruang:<pin>").
CREATE OR REPLACE FUNCTION public.hash_pin(pin TEXT) RETURNS TEXT AS $$
  SELECT encode(digest('beruang:' || pin, 'sha256'), 'hex');
$$ LANGUAGE SQL IMMUTABLE;

-- Generate a unique 6-digit account_id for a user if they don't have one yet.
CREATE OR REPLACE FUNCTION public.ensure_account_id(uid UUID)
RETURNS TEXT AS $$
DECLARE
  existing TEXT;
  new_id TEXT;
  attempts INT := 0;
BEGIN
  SELECT account_id INTO existing FROM public.profiles WHERE id = uid;
  IF existing IS NOT NULL THEN RETURN existing; END IF;

  LOOP
    new_id := lpad((100000 + floor(random() * 900000))::INT::TEXT, 6, '0');
    EXIT WHEN NOT EXISTS (SELECT 1 FROM public.profiles WHERE account_id = new_id);
    attempts := attempts + 1;
    IF attempts > 50 THEN RAISE EXCEPTION 'Could not allocate unique account_id'; END IF;
  END LOOP;

  UPDATE public.profiles SET account_id = new_id WHERE id = uid;
  INSERT INTO public.wallets (user_id, balance) VALUES (uid, 0) ON CONFLICT DO NOTHING;
  RETURN new_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Award points for an activity ('post' = +20, 'comment' = +50, 'friend' = +10).
-- Credits the wallet, the profiles.points snapshot, and the point_events audit log.
CREATE OR REPLACE FUNCTION public.award_points(uid UUID, reason TEXT, amount BIGINT)
RETURNS BIGINT AS $$
DECLARE
  new_balance BIGINT;
BEGIN
  INSERT INTO public.wallets (user_id, balance) VALUES (uid, 0) ON CONFLICT DO NOTHING;
  UPDATE public.wallets SET balance = balance + amount WHERE user_id = uid RETURNING balance INTO new_balance;
  UPDATE public.profiles SET points = new_balance WHERE id = uid;
  INSERT INTO public.point_events (user_id, reason, amount) VALUES (uid, reason, amount);
  RETURN new_balance;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Set (or change) the caller's 4-digit transaction PIN.
CREATE OR REPLACE FUNCTION public.set_points_pin(pin TEXT)
RETURNS VOID AS $$
BEGIN
  IF char_length(pin) <> 4 OR pin !~ '^\d{4}$' THEN
    RAISE EXCEPTION 'PIN harus 4 digit angka.';
  END IF;
  UPDATE public.profiles SET points_pin = public.hash_pin(pin) WHERE id = auth.uid();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Transfer points from the caller to another user's account_id.
-- Validates the sender's PIN and sufficient balance. Returns a JSON result.
CREATE OR REPLACE FUNCTION public.transfer_points(to_account_id TEXT, amount BIGINT, pin TEXT)
RETURNS JSON AS $$
DECLARE
  me UUID := auth.uid();
  my_pin TEXT;
  my_balance BIGINT;
  recipient_id UUID;
BEGIN
  IF amount <= 0 THEN RAISE EXCEPTION 'Nominal harus lebih dari 0.'; END IF;
  IF char_length(pin) <> 4 OR pin !~ '^\d{4}$' THEN RAISE EXCEPTION 'PIN harus 4 digit angka.'; END IF;
  IF me IS NULL THEN RAISE EXCEPTION 'Tidak terautentikasi.'; END IF;
  IF to_account_id IS NULL OR to_account_id = '' THEN RAISE EXCEPTION 'QR tujuan tidak valid.'; END IF;

  SELECT points_pin INTO my_pin FROM public.profiles WHERE id = me;
  IF my_pin IS NULL THEN RAISE EXCEPTION 'Anda belum mengatur PIN transaksi.'; END IF;
  IF my_pin <> public.hash_pin(pin) THEN RAISE EXCEPTION 'PIN salah.'; END IF;

  SELECT id INTO recipient_id FROM public.profiles WHERE account_id = to_account_id;
  IF recipient_id IS NULL THEN RAISE EXCEPTION 'Akun tujuan tidak ditemukan.'; END IF;
  IF recipient_id = me THEN RAISE EXCEPTION 'Tidak bisa transfer ke akun sendiri.'; END IF;

  SELECT balance INTO my_balance FROM public.wallets WHERE user_id = me;
  IF my_balance IS NULL THEN my_balance := 0; END IF;
  IF my_balance < amount THEN RAISE EXCEPTION 'Poin tidak cukup. Saldo: %', my_balance; END IF;

  UPDATE public.wallets SET balance = balance - amount WHERE user_id = me;
  INSERT INTO public.wallets (user_id, balance) VALUES (recipient_id, 0) ON CONFLICT DO NOTHING;
  UPDATE public.wallets SET balance = balance + amount WHERE user_id = recipient_id;
  UPDATE public.profiles SET points = (SELECT balance FROM public.wallets WHERE user_id = me) WHERE id = me;
  UPDATE public.profiles SET points = (SELECT balance FROM public.wallets WHERE user_id = recipient_id) WHERE id = recipient_id;
  INSERT INTO public.transactions (from_id, to_id, amount) VALUES (me, recipient_id, amount);

  RETURN json_build_object('ok', true, 'amount', amount, 'to', recipient_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- ==========================================
-- 6. AUTO-CREATE PROFILE TRIGGER
-- ==========================================

-- Function to automatically create a profile when a new user signs up
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$ BEGIN
  INSERT INTO public.profiles (id, full_name, avatar_url)
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'full_name', 'New Goat'),
    'https://api.dicebear.com/7.x/avataaars/svg?seed=' || NEW.id
  );
  -- Seed the wallet and a unique 6-digit account_id right away.
  INSERT INTO public.wallets (user_id, balance) VALUES (NEW.id, 0) ON CONFLICT DO NOTHING;
  PERFORM public.ensure_account_id(NEW.id);
  RETURN NEW;
END;
 $$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to run the function after a user is created in auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
