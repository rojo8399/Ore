# --- !Ups

create table project_competitions(
  id                      bigserial     not null primary key,
  created_at              timestamp     not null,
  user_id                 bigint        not null references users on delete restrict,
  name                    varchar(255)  not null,
  description             text,
  start_date              timestamp     not null,
  end_date                timestamp     not null,
  is_voting_enabled       boolean       not null default true,
  is_staff_voting_only    boolean       not null default false,
  should_show_vote_count  boolean       not null default true,
  is_sponge_only          boolean       not null default false,
  is_source_required      boolean       not null default false,
  default_votes           int           not null default 1,
  staff_votes             int           not null default 1,
  allowed_entries         int           not null default 1,
  max_entry_total         int           not null default -1
);

# --- !Downs

drop table project_competitions;
