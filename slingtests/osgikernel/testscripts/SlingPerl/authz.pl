#!/usr/bin/perl

#{{{Documentation
=head1 SYNOPSIS

authz perl script. Provides a means of manipulating access control on content
in sling from the command line. This script can be used to get, set, update and
delete content permissions. It also acts as a reference implementation for the
Authz perl library.

=head1 OPTIONS

Usage: perl authz.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --auth (type)                 - Specify auth type. If ommitted, default is used.
 --delete or -d                - delete access control list for node for principal.
 --help or -?                  - view the script synopsis and options.
 --log or -L (log)             - Log script output to specified log file.
 --man or -M                   - view the full script documentation.
 --(no-)addChildNodes          - Grant or deny the addChildNodes privilege
 --(no-)all                    - Grant or deny all above privileges
 --(no-)modifyACL              - Grant or deny the modifyACL privilege
 --(no-)modifyProps            - Grant or deny the modifyProperties privilege
 --(no-)readACL                - Grant or deny the readACL privilege
 --(no-)read                   - Grant or deny the read privilege
 --(no-)removeChilds           - Grant or deny the removeChildNodes privilege
 --(no-)removeNode             - Grant or deny the removeNode privilege
 --(no-)write                  - Grant or deny the write privileges:
                                 modifyProperties,addChildNodes,removeNode,removeChildNodes
 --pass or -p (password)       - Password of user performing content manipulations.
 --principal or -P (principal) - Principal to grant, deny, or delete privilege for.
 --remote or -r (remoteNode)   - specify remote node under JCR root to act on.
 --url or -U (URL)             - URL for system being tested against.
 --user or -u (username)       - Name of user to perform content manipulations as.
 --verbose or -v               - Increase verbosity of output.
 --view or -V                  - view access control list for node.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl authz.pl --man

=head1 Example Usage

=over

=item Authenticate and view the ACL for the /data node:

 perl authz.pl -U http://localhost:8080 -r /data -V -u admin -p admin

=item Authenticate and grant the read privilege to the owner principal, view the result:

 perl authz.pl -U http://localhost:8080 -r /testdata -P owner --read -u admin -p admin -V

=item Authenticate and grant the modifyProps privilege to the everyone principal, view the result:

 perl authz.pl -U http://localhost:8080 -r /testdata -P everyone --modifyProps -u admin -p admin -V

=item Authenticate and deny the addChildNodes privilege to the testuser principal, view the result:

 perl authz.pl -U http://localhost:8080 -r /testdata -P testuser --no-addChildNodes -u admin -p admin -V

=item Authenticate with form based authentication and grant the read and write privileges to the testgroup principal, log the results, including the resulting JSON, to authz.log:

 perl authz.pl -U http://localhost:8080 -r /testdata -P testgroup --read --write -u admin -p admin --auth form -V -L authz.log

=back

=head1 JSR-283 privileges:

The following privileges are not yet supported, but may be soon:

 --(no-)lockManage      - Grant or deny the lockManagement privilege
 --(no-)versionManage   - Grant or deny the versionManagement privilege
 --(no-)nodeTypeManage  - Grant or deny the nodeTypeManagement privilege
 --(no-)retentionManage - Grant or deny the retentionManagement privilege
 --(no-)lifecycleManage - Grant or deny the lifeCycleManagement privilege

=cut
#}}}

#{{{imports
use strict;
use lib qw ( .. );
use LWP::UserAgent ();
use Pod::Usage;
use Sling::Authz;
use Sling::UserAgent;
use Sling::URL;
use Getopt::Long qw(:config bundling);
#}}}

#{{{options parsing
my $auth;
my $delete;
my $help;
my $log;
my $man;
my $password;
my $principal;
my $remoteNode;
my $url = "http://localhost";
my $username;
my $verbose;
my $view;

# privileges:
my $addChildNodes;
my $all;
my $lifecycleManage;
my $lockManage;
my $modifyACL;
my $modifyProps;
my $nodeTypeManage;
my $read;
my $readACL;
my $removeChilds;
my $removeNode;
my $retentionManage;
my $versionManage;
my $write;

GetOptions (
    "addChildNodes!" => \$addChildNodes,
    "all!" => \$all,
    "auth=s" => \$auth,
    "delete|d" => \$delete,
    "help|?" => \$help,
    "lifecycleManage!" => \$lifecycleManage,
    "lockManage!" => \$lockManage,
    "log|L=s" => \$log,
    "man|M" => \$man,
    "modifyACL!" => \$modifyACL,
    "modifyProps!" => \$modifyProps,
    "nodeTypeManage!" => \$nodeTypeManage,
    "pass|p=s" => \$password,
    "principal|P=s" => \$principal,
    "readACL!" => \$readACL,
    "read!" => \$read,
    "remote|r=s" => \$remoteNode,
    "removeChilds!" => \$removeChilds,
    "removeNode!" => \$removeNode,
    "retentionManage!" => \$retentionManage,
    "url|U=s" => \$url,
    "user|u=s" => \$username,
    "versionManage!" => \$versionManage,
    "verbose|v+" => \$verbose,
    "view|V" => \$view,
    "write!" => \$write
) or pod2usage(2);

pod2usage(-exitstatus => 0, -verbose => 1) if $help;
pod2usage(-exitstatus => 0, -verbose => 2) if $man;

$remoteNode = Sling::URL::strip_leading_slash( $remoteNode );

$url =~ s/(.*)\/$/$1/;
$url = ( $url !~ /^http/ ? "http://$url" : "$url" );
#}}}

#{{{ main execution path
my $lwpUserAgent = Sling::UserAgent::get_user_agent( $log, $url, $username, $password, $auth );
my $authz = new Sling::Authz( $url, $lwpUserAgent, $verbose );
if ( defined $delete ) {
    $authz->delete( $remoteNode, $principal, $log );
    Sling::Print::print_result( $authz );
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
    $authz->modify_privileges( $remoteNode, $principal, \@grant_privileges, \@deny_privileges, $log );
    Sling::Print::print_result( $authz, $log );
}
if ( defined $view ) {
    $authz->get_acl( $remoteNode, $log );
    Sling::Print::print_result( $authz, $log );
}
#}}}

1;
