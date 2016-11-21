# --- !Ups

alter table project_competitions add unique (name);

# --- !Downs
