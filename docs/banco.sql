use pjiii;
drop table if exists unijui;
create table unijui(
rg_aluno integer unsigned auto_increment primary key,
nome varchar(100) not null,
cpf varchar (20) not null,
sala varchar(20),
disciplina varchar(60)
);