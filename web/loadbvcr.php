<?php

//folder that holds all the data
$uploadpath = "uploads/";

//create the folder and give appropriate permissions, if it doesn't exist
if (!file_exists($uploadpath))
{
	mkdir($uploadpath);
	chmod($uploadpath,0755);
}

$tdir = "BVCR/";

if (file_exists($uploadpath.$tdir)) { }
else {
   mkdir($uploadpath.$tdir);
   chmod($uploadpath.$tdir,0755);
}

$user = $_REQUEST["U"];
$repo = $_REQUEST["R"];
$dlm = $_REQUEST["D"];

if ($dlm == "") $dlm = 0;

if (!file_exists($uploadpath.$tdir.$repo)) {
   mkdir($uploadpath.$tdir.$repo);
   chmod($uploadpath.$tdir.$repo,0755);
}

$dirpath = $uploadpath.$tdir.$repo;

$files = array();
$dh = opendir($dirpath);
while (($filename = readdir($dh)) !== false) {
   $f = $dirpath . "/" . $filename;
   if ($filename != "." && $filename != ".." && $filename != $user && filemtime($f) > $dlm) {
      $files[] = $f;
    }
}
sort($files);

$ct = count($files);
$hdr = "$ct";
$len = 0;
foreach ($files as $f) {
   $fl = filesize($f);
   $len += $fl;
   $hdr = "$hdr $fl";
 }
$hdr .= "\n";
$len += strlen($hdr);

header('Content-Type: application/octet-stream');
header('Content-Disposition: inline');
header('Content-Transfer-Encoding: binary');
header('Content-Length: ' . $len);
echo($hdr);
foreach ($files as $f) {
   readfile($f);
}

?>
