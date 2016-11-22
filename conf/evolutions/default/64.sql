# --- !Ups

alter table project_competitions add column time_zone varchar(255) not null default 'EST';
alter table project_competitions alter column time_zone drop default;

# --- !Downs

alter table project_competitions drop column time_zone;
