-- bitmeds sync. Run once in Supabase → SQL Editor for the "128bit play" project (safe to run again).
-- One row per account with that account's meds, history, labs and settings. Row level security means
-- each signed-in account can only ever read or write its own row; anonymous users get nothing.
create table if not exists public.bitmeds_state (
  user_id uuid primary key references auth.users on delete cascade,
  data jsonb not null default '{}',
  updated_at timestamptz not null default now()
);
alter table public.bitmeds_state enable row level security;
drop policy if exists "bitmeds_state own row select" on public.bitmeds_state;
drop policy if exists "bitmeds_state own row insert" on public.bitmeds_state;
drop policy if exists "bitmeds_state own row update" on public.bitmeds_state;
drop policy if exists "bitmeds_state own row delete" on public.bitmeds_state;
create policy "bitmeds_state own row select" on public.bitmeds_state for select to authenticated using ((select auth.uid()) = user_id);
create policy "bitmeds_state own row insert" on public.bitmeds_state for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "bitmeds_state own row update" on public.bitmeds_state for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "bitmeds_state own row delete" on public.bitmeds_state for delete to authenticated using ((select auth.uid()) = user_id);
revoke all on public.bitmeds_state from anon;
grant select, insert, update, delete on public.bitmeds_state to authenticated;
