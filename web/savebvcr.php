<?php

//To add extensions to the list of permitted extensions, add more
//options of the form ($ext == "my extension here") to the if statement
//in line 21

//folder that holds all the data
$uploadpath = "uploads/";

//create the folder and give appropriate permissions, if it doesn't exist
if (!file_exists($uploadpath))
{
	mkdir($uploadpath);
	chmod($uploadpath,0755);
}

$tdir = "BVCR/";

$ext = substr($_FILES["file"]["name"], strrpos($_FILES["file"]["name"], '.') + 1);

if (($ext == "bvcr")) {
   if ($_FILES["file"]["error"] > 0) { }
   else {
      if (file_exists($uploadpath.$tdir)) { }
      else {
	 mkdir($uploadpath.$tdir);
	 chmod($uploadpath.$tdir,0755);
       }

      $user = $_POST["U"];
      $repo = $_POST["R"];

      if (!file_exists($uploadpath.$tdir.$repo)) {
	 mkdir($uploadpath.$tdir.$repo);
	 chmod($uploadpath.$tdir.$repo,0755);
       }

      $dirpath = $uploadpath.$tdir.$repo;
      $filename = $dirpath . "/" . $user;
      $tfile = $filename . ".tmp";

      if (move_uploaded_file($_FILES["file"]["tmp_name"],$tfile)) {
	 if (rename($tfile,$filename)) {
	    echo $filename;
	  }
       }
    }
}
else echo "Extension not permitted";

?>
