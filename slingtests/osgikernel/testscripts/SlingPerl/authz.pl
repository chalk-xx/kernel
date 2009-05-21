#!/usr/bin/perl

=head1 NAME

authz perl script. Provides a means of manipulating access control on content
in sling from the command line.

=head1 ABSTRACT

This script can be used to get, set, update and delete permissions on content
in sling from the command line. It also acts as a reference implementation for
the Authz perl library.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use LWP::UserAgent ();
use Sling::Authz;
use Sling::UserAgent;
use Sling::Util;
use Getopt::Long qw(:config bundling);
#}}}

#{{{sub HELP_MESSAGE
sub HELP_MESSAGE {
    my ( $out, $optPackage, $optVersion, $switches ) = @_;
    Sling::Util::help_header( $0, $switches );
    print "-d                     - delete access control list for node for principal.\n";
    print "-v                     - view access control list for node.\n";
    print "-p {password}          - Password of user performing content manipulations.\n";
    print "-u {username}          - Name of user to perform content manipulations as.\n";
    print "-D {remoteDest}        - specify remote destination under JCR root to act on.\n";
    print "-L {log}               - Log script output to specified log file.\n";
    print "-P {principal}         - Principal to grant, deny, or delete privilege for.\n";
    print "-U {URL}               - URL for system being tested against.\n";
    print "--auth {type}          - Specify auth type. If ommitted, default is used.\n";
    print "--(no-)read            - Grant or deny the read privilege\n";
    print "--(no-)modifyProps     - Grant or deny the modifyProperties privilege\n";
    print "--(no-)addChildNodes   - Grant or deny the addChildNodes privilege\n";
    print "--(no-)removeNode      - Grant or deny the removeNode privilege\n";
    print "--(no-)removeChilds    - Grant or deny the removeChildNodes privilege\n";
    print "--(no-)write           - Grant or deny the write privileges\n";
    print "--(no-)readACL         - Grant or deny the readACL privilege\n";
    print "--(no-)modifyACL       - Grant or deny the modifyACL privilege\n";
    print "--(no-)lockManagement  - Grant or deny the lockManagement privilege\n";
    print "--(no-)versionManage   - Grant or deny the versionManagement privilege\n";
    print "--(no-)nodeTypeManage  - Grant or deny the nodeTypeManagement privilege\n";
    print "--(no-)retentionManage - Grant or deny the retentionManagement privilege\n";
    print "--(no-)lifecycleManage - Grant or deny the lifeCycleManagement privilege\n";
    print "--(no-)all             - Grant or deny all privileges\n";
    Sling::Util::help_footer( $0 );
}
#}}}

#{{{options parsing
my $auth;
my $delete;
my $view;
my $url = "http://localhost";
my $log;
my $username;
my $password;
my $remoteDest;
my $principal;

# privileges:
my $read;
my $modifyProps;
my $addChildNodes;
my $removeNode;
my $removeChilds;
my $write;
my $readACL;
my $modifyACL;
my $lockManagement;
my $versionManage;
my $nodeTypeManage;
my $retentionManage;
my $lifecycleManage;
my $all;

GetOptions ( "v" => \$view,                           "U=s" => \$url,
	     "p=s" => \$password,                     "D=s" => \$remoteDest,
	     "u=s" => \$username,                     "L=s" => \$log,
	     "auth=s" => \$auth,                      "help" => \&HELP_MESSAGE,
	     "read!" => \$read,                       "modifyProps!" => \$modifyProps,
	     "addChildNodes!" => \$addChildNodes,     "removeNode!" => \$removeNode,
	     "removeChilds!" => \$removeChilds,       "write!" => \$write,
             "readACL!" => \$readACL,                 "modifyACL!" => \$lockManagement,
	     "versionManage!" => \$versionManage,     "nodeTypeManage!" => \$nodeTypeManage,
	     "retentionManage!" => \$retentionManage, "lifecycleManage!" => \$lifecycleManage,
	     "all!" => \$all,                         "P=s" => \$principal,
	     "d" => \$delete );

# Strip leading slashes from the remoteDest and remoteSrc
$remoteDest =~ s/^\///;

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );
#}}}

#{{{ main execution path
my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
my $authz = new Sling::Authz( $url, $lwpUserAgent );
if ( defined $delete ) {
    $authz->delete( $remoteDest, $principal, $log );
    print $authz->{ 'Message' } . "\n";
}
if ( defined $read ) {
    $read ? $authz->grant_read( $remoteDest, $principal, $log ) : $authz->deny_read( $remoteDest, $principal, $log ); 
    print $authz->{ 'Message' } . "\n";
}
if ( defined $modifyProps ) {
    $modifyProps ? $authz->grant_modifyProperties( $remoteDest, $principal, $log ) : $authz->deny_modifyProperties( $remoteDest, $principal, $log ); 
    print $authz->{ 'Message' } . "\n";
}
if ( defined $view ) {
    $authz->get_acl( $remoteDest, $log );
    if ( ! defined $log ) {
        print $authz->{ 'Message' } . "\n";
    }
}
#}}}

1;
