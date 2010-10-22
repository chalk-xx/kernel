#!/usr/bin/perl

# takes a file path to a csv file as an argument and then adds the users from that file to nakamura
$clean=100;
$counter_time=0;
$counter=0;
$first=time;

$file = $ARGV[0];

open (F, $file) || die ("Could not open $file!");

while ($line = <F>)
{
  ($name,$password) = split ',', $line;
  if ($counter_time==$clean) {
      $last=time;
      $diff=$last-$first;
      print "$counter,$diff\n";
      $first=$last;
      $counter_time=0;
  }
  $counter_time++;
  $counter++;
  $val=&create_profile($name); 
  system ("curl $val -F:name=$name -Fpwd=test -FpwdConfirm=test http://admin:admin\@localhost:8080/system/userManager/user.create.html 2> /dev/null  >/dev/null;");
}

close (F);

sub create_profile{
my ($tester)=@_;  
  $ret=  "-F \":sakai:profile-import={\'basic\': {\'access\': \'everybody\', \'elements\': {\'email\': {\'value\': \'$tester\@sakai.invalid\'}, \'firstName\': {\'value\': '$tester'}, 'lastName': {'value': \'$tester\'}}}}\"";
return $ret;
}



