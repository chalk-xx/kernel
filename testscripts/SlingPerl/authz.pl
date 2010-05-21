#!/usr/bin/perl

#{{{imports
use warnings;
use strict;
use Carp;
use lib qw ( .. );
use version; our $VERSION = qv('0.0.1');
use Getopt::Long qw(:config bundling);
use Pod::Usage;
use Sling::Authn;
use Sling::Authz;
use Sling::URL;

#}}}

#{{{options parsing
my $auth;
my $delete;
my $help;
my $log;
my $man;
my $password;
my $principal;
my $remote_node;
my $url;
my $username;
my $verbose;
my $view;

# privileges:
my $add_child_nodes;
my $all;
my $life_cycle_manage;
my $lock_manage;
my $modify_acl;
my $modify_props;
my $node_type_manage;
my $read;
my $read_acl;
my $remove_childs;
my $remove_node;
my $retention_manage;
my $version_manage;
my $write;

GetOptions(
    'addChildNodes!'   => \$add_child_nodes,
    'all!'             => \$all,
    'auth=s'           => \$auth,
    'delete|d'         => \$delete,
    'help|?'           => \$help,
    'lifecycleManage!' => \$life_cycle_manage,
    'lockManage!'      => \$lock_manage,
    'log|L=s'          => \$log,
    'man|M'            => \$man,
    'modifyACL!'       => \$modify_acl,
    'modifyProps!'     => \$modify_props,
    'nodeTypeManage!'  => \$node_type_manage,
    'pass|p=s'         => \$password,
    'principal|P=s'    => \$principal,
    'readACL!'         => \$read_acl,
    'read!'            => \$read,
    'remote|r=s'       => \$remote_node,
    'removeChilds!'    => \$remove_childs,
    'removeNode!'      => \$remove_node,
    'retentionManage!' => \$retention_manage,
    'url|U=s'          => \$url,
    'user|u=s'         => \$username,
    'versionManage!'   => \$version_manage,
    'verbose|v+'       => \$verbose,
    'view|V'           => \$view,
    'write!'           => \$write
) or pod2usage(2);

if ($help) { pod2usage( -exitstatus => 0, -verbose => 1 ); }
if ($man)  { pod2usage( -exitstatus => 0, -verbose => 2 ); }

$remote_node = Sling::URL::strip_leading_slash($remote_node);

$url = Sling::URL::url_input_sanitize($url);

#}}}

#{{{ main execution path
my $authn =
  new Sling::Authn( $url, $username, $password, $auth, $verbose, $log );
my $authz = new Sling::Authz( \$authn, $verbose, $log );
if ( defined $delete ) {
    $authz->delete( $remote_node, $principal );
    Sling::Print::print_result($authz);
}
my @grant_privileges;
my @deny_privileges;
if ( defined $read ) {
    $read
      ? push @grant_privileges, 'read'
      : push @deny_privileges, 'read';
}
if ( defined $modify_props ) {
    $modify_props
      ? push @grant_privileges, 'modifyProperties'
      : push @deny_privileges, 'modifyProperties';
}
if ( defined $add_child_nodes ) {
    $add_child_nodes
      ? push @grant_privileges, 'addChildNodes'
      : push @deny_privileges, 'addChildNodes';
}
if ( defined $remove_node ) {
    $remove_node
      ? push @grant_privileges, 'removeNode'
      : push @deny_privileges, 'removeNode';
}
if ( defined $remove_childs ) {
    $remove_childs
      ? push @grant_privileges, 'removeChildNodes'
      : push @deny_privileges, 'removeChildNodes';
}
if ( defined $write ) {
    $write
      ? push @grant_privileges, 'write'
      : push @deny_privileges, 'write';
}
if ( defined $read_acl ) {
    $read_acl
      ? push @grant_privileges, 'readAccessControl'
      : push @deny_privileges, 'readAccessControl';
}
if ( defined $modify_acl ) {
    $modify_acl
      ? push @grant_privileges, 'modifyAccessControl'
      : push @deny_privileges, 'modifyAccessControl';
}

# Privileges that may become available in due course:
# if ( defined $lock_manage ) {
# $lock_manage ? push ( @grant_privileges, 'lockManagement' ) : push ( @deny_privileges, 'lockManagement' );
# }
# if ( defined $version_manage ) {
# $version_manage ? push ( @grant_privileges, 'versionManagement' ) : push ( @deny_privileges, 'versionManagement' );
# }
# if ( defined $node_type_manage ) {
# $node_type_manage ? push ( @grant_privileges, 'nodeTypeManagement' ) : push ( @deny_privileges, 'nodeTypeManagement' );
# }
# if ( defined $retention_manage ) {
# $retention_manage ? push ( @grant_privileges, 'retentionManagement' ) : push ( @deny_privileges, 'retentionManagement' );
# }
# if ( defined $life_cycle_manage ) {
# $life_cycle_manage ? push ( @grant_privileges, 'lifecycleManagement' ) : push ( @deny_privileges, 'lifecycleManagement' );
# }
if ( defined $all ) {
    $all ? push @grant_privileges, 'all' : push @deny_privileges, 'all';
}
if ( @grant_privileges || @deny_privileges ) {
    $authz->modify_privileges( $remote_node, $principal, \@grant_privileges,
        \@deny_privileges );
    Sling::Print::print_result($authz);
}
if ( defined $view ) {
    $authz->get_acl($remote_node);
    Sling::Print::print_result($authz);
}

#}}}

1;

__END__

#{{{Documentation

=head1 NAME

authz.pl

=head1 SYNOPSIS

authz perl script. Provides a means of manipulating access control on content
in sling from the command line. This script can be used to get, set, update and
delete content permissions. It also acts as a reference implementation for the
Authz perl library.

=head1 OPTIONS

Usage: perl authz.pl [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]
The following options are accepted:

 --auth (type)                  - Specify auth type. If ommitted, default is used.
 --delete or -d                 - delete access control list for node for principal.
 --help or -?                   - view the script synopsis and options.
 --log or -L (log)              - Log script output to specified log file.
 --man or -M                    - view the full script documentation.
 --(no-)addChildNodes           - Grant or deny the addChildNodes privilege
 --(no-)all                     - Grant or deny all above privileges
 --(no-)modifyACL               - Grant or deny the modifyACL privilege
 --(no-)modifyProps             - Grant or deny the modifyProperties privilege
 --(no-)readACL                 - Grant or deny the readACL privilege
 --(no-)read                    - Grant or deny the read privilege
 --(no-)removeChilds            - Grant or deny the removeChildNodes privilege
 --(no-)removeNode              - Grant or deny the removeNode privilege
 --(no-)write                   - Grant or deny the write privileges:
                                  modifyProperties,addChildNodes,removeNode,removeChildNodes
 --pass or -p (password)        - Password of user performing content manipulations.
 --principal or -P (principal)  - Principal to grant, deny, or delete privilege for.
 --remote or -r (remoteNode)    - specify remote node under JCR root to act on.
 --url or -U (URL)              - URL for system being tested against.
 --user or -u (username)        - Name of user to perform content manipulations as.
 --verbose or -v or -vv or -vvv - Increase verbosity of output.
 --view or -V                   - view access control list for node.

Options may be merged together. -- stops processing of options.
Space is not required between options and their arguments.
For full details run: perl authz.pl --man

=head1 USAGE

=over

=item Authenticate and view the ACL for the /data node:

 perl authz.pl -U http://localhost:8080 -r /data -V -u admin -p admin

=item Authenticate and grant the read privilege to the owner principal, view the result:

 perl authz.pl -U http://localhost:8080 -r /testdata -P owner --read -u admin -p admin -V

=item Authenticate and grant the modifyProps privilege to the everyone principal, view the result:

 perl authz.pl -U http://localhost:8080 -r /testdata -P everyone --modifyProps -u admin -p admin -V

=item Authenticate and deny the addChildNodes privilege to the testuser principal, view the result:

 perl authz.pl -U http://localhost:8080 -r /testdata -P testuser --no-addChildNodes -u admin -p admin -V

=item Authenticate with form based authentication and grant the read and write privileges to the g-testgroup principal, log the results, including the resulting JSON, to authz.log:

 perl authz.pl -U http://localhost:8080 -r /testdata -P g-testgroup --read --write -u admin -p admin --auth form -V -L authz.log

=back

=head1 JSR-283 privileges:

The following privileges are not yet supported, but may be soon:

 --(no-)lockManage      - Grant or deny the lockManagement privilege
 --(no-)versionManage   - Grant or deny the versionManagement privilege
 --(no-)nodeTypeManage  - Grant or deny the nodeTypeManagement privilege
 --(no-)retentionManage - Grant or deny the retentionManagement privilege
 --(no-)lifecycleManage - Grant or deny the lifeCycleManagement privilege

=head1 DESCRIPTION

authz perl script. Provides a means of manipulating access control on content
in sling from the command line. This script can be used to get, set, update and
delete content permissions. It also acts as a reference implementation for the
Authz perl library.

=head1 REQUIRED ARGUMENTS

None.

=head1 DIAGNOSTICS

Run with multiple -v options to enable verbose output.

=head1 EXIT STATUS

1 on success, otherwise failure.

=head1 CONFIGURATION

None needed.

=head1 DEPENDENCIES

Carp; Getopt::Long; Pod::Usage; Sling::Authn; Sling::Authz; Sling::URL;

=head1 INCOMPATIBILITIES

None known (^_-)

=head1 BUGS AND LIMITATIONS

None known (^_-)

=head1 AUTHOR

Daniel Parry -- daniel@caret.cam.ac.uk

=head1 LICENSE AND COPYRIGHT

   Copyright 2009 Daniel David Parry

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

=cut

#}}}
