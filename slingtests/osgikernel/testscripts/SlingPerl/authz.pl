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
    print "--(no-)write           - Grant or deny the write privileges (modifyProperties,addChildNodes,removeNode,removeChildNodes)\n";
    print "--(no-)readACL         - Grant or deny the readACL privilege\n";
    print "--(no-)modifyACL       - Grant or deny the modifyACL privilege\n";
    # JSR-283 privileges:
    # print "--(no-)lockManage      - Grant or deny the lockManagement privilege\n";
    # print "--(no-)versionManage   - Grant or deny the versionManagement privilege\n";
    # print "--(no-)nodeTypeManage  - Grant or deny the nodeTypeManagement privilege\n";
    # print "--(no-)retentionManage - Grant or deny the retentionManagement privilege\n";
    # print "--(no-)lifecycleManage - Grant or deny the lifeCycleManagement privilege\n";
    print "--(no-)all             - Grant or deny all above privileges\n";
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
my $lockManage;
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
             "readACL!" => \$readACL,                 "modifyACL!" => \$modifyACL,
	     "versionManage!" => \$versionManage,     "nodeTypeManage!" => \$nodeTypeManage,
	     "retentionManage!" => \$retentionManage, "lifecycleManage!" => \$lifecycleManage,
	     "all!" => \$all,                         "lockManage!" => \$lockManage,
	     "P=s" => \$principal,                    "d" => \$delete );

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
my @grant_privileges;
my @deny_privileges;
if ( defined $read ) {
    $read ? push ( @grant_privileges, "read" ) : push ( @deny_privileges, "read" ); 
}
if ( defined $modifyProps ) {
    $modifyProps ? push ( @grant_privileges, "modifyProperties" ) : push ( @deny_privileges, "modifyProperties" );
}
if ( defined $addChildNodes ) {
    $addChildNodes ? push ( @grant_privileges, "addChildNodes" ) : push ( @deny_privileges, "addChildNodes" ); 
}
if ( defined $removeNode ) {
    $removeNode ? push ( @grant_privileges, "removeNode" ) : push ( @deny_privileges, "removeNode" ); 
}
if ( defined $removeChilds ) {
    $removeChilds ? push ( @grant_privileges, "removeChildNodes" ) : push ( @deny_privileges, "removeChildNodes" ); 
}
if ( defined $write ) {
    $write ? push ( @grant_privileges, "write" ) : push ( @deny_privileges, "write" ); 
}
if ( defined $readACL ) {
    $readACL ? push ( @grant_privileges, "readAccessControl" ) : push ( @deny_privileges, "readAccessControl" ); 
}
if ( defined $modifyACL ) {
    $modifyACL ? push ( @grant_privileges, "modifyAccessControl" ) : push ( @deny_privileges, "modifyAccessControl" ); 
}
# Privileges that may become available in due course:
# if ( defined $lockManage ) {
    # $lockManage ? push ( @grant_privileges, "lockManagement" ) : push ( @deny_privileges, "lockManagement" ); 
# }
# if ( defined $versionManage ) {
    # $versionManage ? push ( @grant_privileges, "versionManagement" ) : push ( @deny_privileges, "versionManagement" ); 
# }
# if ( defined $nodeTypeManage ) {
    # $nodeTypeManage ? push ( @grant_privileges, "nodeTypeManagement" ) : push ( @deny_privileges, "nodeTypeManagement" ); 
# }
# if ( defined $retentionManage ) {
    # $retentionManage ? push ( @grant_privileges, "retentionManagement" ) : push ( @deny_privileges, "retentionManagement" ); 
# }
# if ( defined $lifecycleManage ) {
    # $lifecycleManage ? push ( @grant_privileges, "lifecycleManagement" ) : push ( @deny_privileges, "lifecycleManagement" ); 
# }
if ( defined $all ) {
    $all ? push ( @grant_privileges, "all" ) : push ( @deny_privileges, "all" ); 
}
if ( @grant_privileges || @deny_privileges ) {
    $authz->modify_privileges( $remoteDest, $principal, \@grant_privileges, \@deny_privileges, $log );
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
