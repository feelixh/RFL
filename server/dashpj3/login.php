<?php 
session_start();
include("conexao_new.php");
?>
<!DOCTYPE>
<html lang="pt-br">

<head>
<!-- Generated by javadoc (version 1.7.0_51) on Fri Apr 17 22:20:24 BRT 2015 -->
<meta http-equiv="Content-Type" content="text/html" charset="UTF-8">
<title>Login</title>
<link href="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.1.2/css/bootstrap.min.css" rel="stylesheet">  
<link href="https://use.fontawesome.com/releases/v5.3.1/css/all.css" rel="stylesheet">  
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js"></script>
<link rel="stylesheet" type="text/css" href="style.css">
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

    <?php
    //    ""

 
 
    if(isset($_POST['login'])){
      $login = $_POST['login'];
      $senha = $_POST['senha'];
      $sql = "SELECT id from $dbname WHERE login = '$login' AND senha = '$senha'";
      $result = mysql_query($sql) or die(mysql_error());
      if (mysql_num_rows($result) > 0) {
        $_SESSION['login'] = $login;
        $_SESSION['senha'] = $senha;
        header('Location: dash.php');
      } else{
       $flag = 1;
      }
           
    }
      ?>
    

    <form method="post" class="form-login">
        <label for="login"> Login </label>
        <input type="text" name="login"><br>
        <label for="senha"> Senha </label>
        <input type="password" name="senha">
        <input class="btn btn-primary" type="submit">
        <?php
    if ($flag == 1){
    
      echo '<p style="color: red; text-align: center"> Login ou senha incorretos</p>';
    }
    ?>
    </form>

    

</body>


</html>