<?php
include("verifica_login.php");
include("conexao_new.php");
?>
<!DOCTYPE html>
<!-- NewPage -->
<html lang="pt">
<head>
<!-- Generated by javadoc (version 1.7.0_51) on Fri Apr 17 22:20:24 BRT 2015 -->
<meta http-equiv="Content-Type" content="text/html" charset="UTF-8">
<title>Dashboard</title>
<link href="//netdna.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" rel="stylesheet">  
<script src="//netdna.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
<link href="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.1.2/css/bootstrap.min.css" rel="stylesheet">
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css" > 
<link href="https://use.fontawesome.com/releases/v5.3.1/css/all.css" rel="stylesheet"> 
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js"></script>
<link rel="stylesheet" type="text/css" href="css/bootstrap.min.css">
<link rel="stylesheet" type="text/css" href="style.css">


<script>

</script>
<style type="text/css" >
	.container {
  padding: 2rem 0rem;
}

h4 {
  margin: 2rem 0rem 1rem;
}

.table-image {
  td, th {
    vertical-align: middle;
  }
}

</style>
</head>
<body>
<header> 

<nav class="navbar navbar-expand-sm navbar-dark fixed-top bg-dark" style="height: 50px; margin-bottom:50px">
<div class="col-md-10">

</div>
<div style="color: white; right: 10px;" class="col-md-2">
  <?php
      echo "<b>Bem vindo, $logado </b>";
  ?>
</div>
 <div id="box-config">
    <button id="config-button" class="dropdown" type="button" data-toggle="dropdown" aria-expanded="true"> 
        <span style="color: white" class="glyphicon glyphicon-option-vertical"></span>
    </button>
    <div class="dropdown-menu" aria-labelledby="config-button">
            <a class="dropdown-item" id="logoff" href="#">Sair</a>    
    </div>
  </div>
</nav>

<?php
if($_GET){
if (isset($_GET['id'])) {
    $id =  $_GET['id'];
    $op =  $_GET['op'];
    $sql;
    if($op==1){
      $sql = "UPDATE reconhecimento set ativo = 1 WHERE id = '$id'";
      $result = mysql_query($sql) or die(mysql_error());
      

    }else if($op == 2){
      $sql = "UPDATE reconhecimento set ativo = 2 where id = '$id'";
      $result = mysql_query($sql) or die(mysql_error());
    }

    if(mysql_query($sql)){
    	?>
                <script>
                   alert("Alterações realizadas com sucesso!");
                   </script>
                <?php
    }else{
    	?>
                <script>
                   alert("Erro ao realizar alterações!");
                   </script>
                <?php
    }

      }
}



?>
<div id="ajuste" style="height: 50px; display:block"> </div>
<div class="container">
	<div class="row">
      <div class="dropdown" >
          <button class="btn btn-secondary dropdown-toggle" type="button" id="selecionar" data-toggle="dropdown" aria-expanded="true">
              Selecionar
          </button>
          <div class="dropdown-menu" aria-labelledby="selecionar">
            <a class="dropdown-item" id="selecionar0" href="#">Pendentes</a>
            <a class="dropdown-item" id="selecionar1" href="#">Aprovados</a>
            <a class="dropdown-item" id="selecionar2" href="#">Rejeitados</a>
          </div>
      </div>     
  </div>
  <div class="row" style="padding-bottom:10px;"> 
    <div class="col-md-12"><h1 style="text-align: center" id="titulo"> Pendentes </h1>   
    </div>
  </div>
  <div class="row">
    <div class="col-12">
    <table class="table table-image">
      <thead>
        <tr>
          <th scope="col">Image</th>
          <th scope="col">Nome</th>
          <th scope="col">Identificação</th>
          <th scope="col">Ações</th>
        </tr>
      </thead>
      <tbody>


<?php
$sql = "select r.id, r.rg_aluno, r.cpf, r.imagem, r.ativo, r.tracker, u.NOME_ALUNO from reconhecimento as r inner join unijui as u on u.matr_aluno = r.rg_aluno where r.ativo = 0 group by u.NOME_ALUNO ;";
if (isset($_GET['filter'])) {
	if($_GET['filter']==0){
		$sql = "select r.id, r.rg_aluno, r.cpf, r.imagem, r.ativo, r.tracker, u.NOME_ALUNO from reconhecimento as r inner join unijui as u on u.matr_aluno = r.rg_aluno where r.ativo = 0 group by u.NOME_ALUNO ;";
	}else if($_GET['filter']==1){
		$sql = "select r.id, r.rg_aluno, r.cpf, r.imagem, r.ativo, r.tracker, u.NOME_ALUNO from reconhecimento as r inner join unijui as u on u.matr_aluno = r.rg_aluno where r.ativo = 1 group by u.NOME_ALUNO ;";
	}else if($_GET['filter']==2){
		$sql = "select r.id, r.rg_aluno, r.cpf, r.imagem, r.ativo, r.tracker, u.NOME_ALUNO from reconhecimento as r inner join unijui as u on u.matr_aluno = r.rg_aluno where r.ativo = 2 group by u.NOME_ALUNO ;";
	}
	}
	

                
                // Executa consulta SQL
                $query = mysql_query($sql);
                // Enquanto houverem registros no banco de dados
                while($row = mysql_fetch_array($query)) {
                   echo "<tr id=\"".$row['id']."\">";
                  echo "<td style=\"text-align: left; vertical-align: middle;\" scope=\"row\" class=\"w-25\"> <img style=\"width: 80%; height: auto;\" src=\"../".$row['imagem']."\" class=\"img-fluid img-thumbnail\" alt=\"Sheep\"></td>";
                  echo "<td style=\"text-align: left; vertical-align: middle;\" scope=\"row\" class=\"w-25\">".$row['NOME_ALUNO']."</td>";
                 echo "<td style=\"text-align: left; vertical-align: middle;\" scope=\"row\" class=\"w-25\">".$row['rg_aluno']."</td>";
?>
                  <td style="text-align: left; vertical-align: middle;">
                    <div class="text-center">
                      <a href="dash.php?id=<?php echo $row['id'] ?>&op=1"><i class="fas fa-user-check" style="padding-right: 20%; color: black"></i></a>
                      <a href="dash.php?id=<?php echo $row['id'] ?>&op=2"><i class="fas fa-user-times"  style="color: black"></i></a>
                    </div>
                    
                  </td>
<?php

                  echo "</tr>";
                }
                ?>

		  </tbody>
		</table>   
    </div>
  </div>
</div>

<script type="text/javascript">
$( document ).ready(function() {
    $("#selecionar0").click( function(){
    	location.href="dash.php?filter=0";
           }
      );
});

$( document ).ready(function() {
    $("#selecionar1").click( function(){
    	location.href="dash.php?filter=1";
           }
      );
});

$( document ).ready(function() {
    $("#selecionar2").click( function(){
    	location.href="dash.php?filter=2";
           }
      );
});

$( document ).ready(function() {
  if (/filter=0/.test(window.location.href)){
      $('div h1').text('Pendentes');
    }
});

$( document ).ready(function() {
 if (/filter=1/.test(window.location.href)){
      $('div h1').text('Aprovados');
    }
});

$( document ).ready(function() {
  if (/filter=2/.test(window.location.href)){
      $('div h1').text('Rejeitados');
    }
});

$( document ).ready(function() {
    $("#logoff").click( function(){   	
      location.href="logoff.php";
           }
      );
});
</script>
<script src="http://code.jquery.com/jquery-latest.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>
</body>
</html>
