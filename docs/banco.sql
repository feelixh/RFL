use pjiii;
drop table if exists unijui;
create table unijui(
rg_aluno integer unsigned auto_increment primary key,
nome varchar(100) not null,
cpf varchar (20) not null,
sala varchar(20),
disciplina varchar(60)
);

drop table if exists rfl;
create table rfl(
cpf varchar(15) not null primary key,
imagem varchar(200) not null,
id_imagem long not null
);

insert into unijui (rg_aluno, nome, cpf, sala, disciplina) values
(123456, 'Mateus Diel', '01589116025', 'DT201', 'Projeto Integrador 2'),
(789654, 'Felix Hoffmann', '45643135465', 'RD96', 'Computação Gráfica'),
(657123, 'Talles Viecilli', '78645649816', 'RF200', 'Linguagem de Programação'),
(894564, 'Guilherme Cargenlutti', '52454454596', 'OP350', 'Introdução à Algo'),
(311282, 'João Pedro Cargnelutti', '89765456455', 'LK985', 'Lógica de Programação');

