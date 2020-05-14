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
//check if it is a working set or some logfiles
$workingset = $_POST["set"];


$ext = substr($_FILES["file"]["name"], strrpos($_FILES["file"]["name"], '.') + 1);
if (($ext == "txt")||($ext=="png")||($ext=="xml")||($ext=="csv") || ($ext == "buda") || ($ext = "log"))
  {
if ($workingset=="0")
{
  if ($_FILES["file"]["error"] > 0) { }
  else {
		$user = $_POST["user"];

		if (file_exists($uploadpath.$user)) { }
		else {
			mkdir($uploadpath.$user);
			chmod($uploadpath.$user,0755);
		}
		$runid = $_POST["runid"];

		if (!file_exists($uploadpath.$user. "/" . $runid)) {
			mkdir($uploadpath.$user. "/" . $runid);
			chmod($uploadpath.$user. "/" . $runid,0755);
		}
		$dirpath = $uploadpath.$user. "/" . $runid;

		$filesindir = iterator_count(new DirectoryIterator($dirpath."/")) - 2;

		$filename = $dirpath . "/" . $user . "_" . $filesindir . "_" . $_FILES["file"]["name"];

		if (file_exists($filename)) { }
		else {
		   move_uploaded_file($_FILES["file"]["tmp_name"], $filename);
		   chmod($filename,0664);
		   echo $filename;
		 }
   }

}
else {
	$temppath = $uploadpath . "WorkingSets/";
	if (!file_exists($temppath)) {
			mkdir($temppath);
			chmod($temppath,0755);
	}
	$temppath = $temppath . mt_rand() . mt_rand();
	if (!file_exists($temppath)) {
			mkdir($temppath);
			chmod($temppath,0755);
	}

	$filename = $temppath . "/" . $_FILES["file"]["name"];
	if (file_exists($filename)) { }
	else {
		move_uploaded_file($_FILES["file"]["tmp_name"], $filename);
		echo $filename;
	}
  }

}

else echo "Extension not permitted";
?>
