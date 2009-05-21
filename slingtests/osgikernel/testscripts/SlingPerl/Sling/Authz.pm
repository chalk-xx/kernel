#!/usr/bin/perl

package Sling::Authz;

=head1 NAME

Authz - content related functionality for Sling implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST content methods

=head2 Available privliges

=over 

=item jcr:read - the privilege to retrieve a node and get its properties and their values.

=item jcr:modifyProperties - the privilege to create, modify and remove the properties of a node.

=item jcr:addChildNodes - the privilege to create child nodes of a node.

=item jcr:removeNode - the privilege to remove a node.

=item jcr:removeChildNodes the privilege to remove child nodes of a node.

=item jcr:write an aggregate privilege that contains:

 jcr:modifyProperties
 jcr:addChildNodes
 jcr:removeNode
 jcr:removeChildNodes

=item jcr:readAccessControl the privilege to get the access control policy of a node.

=item jcr:modifyAccessControl the privilege to modify the access control policies of a node.

=item jcr:lockManagement the privilege to lock and unlock a node.

=item jcr:versionManagment the privilege to perform versioning operations on a node.

=item jcr:nodeTypeManagement the privilege to add and remove mixin node types and change the primary node type of a node.

=item jcr:retentionManagement the privilege to perform retention management operations on a node.

=item jcr:lifecycleManagement the privilege to perform lifecycle operations on a node.

=item jcr:all an aggregate privilege that contains all predefined privileges.

 jcr:read
 jcr:write
 jcr:readAccessControl
 jcr:modifyAccessControl
 jcr:lockManagement
 jcr:versionManagement
 jcr:nodeTypeManagement
 jcr:retentionManagement
 jcr:lifecycleManagement

=back

Note: In order to actually remove a node, jcr:removeNode is required on that node and
jcr:removeChildNodes on the parent node. The distinction is provided in order
to reflect implementations that internally model "remove" as a "delete" instead
of an "unlink". A repository that uses the "delete" model can have
jcr:removeChildNodes in every access control policy, so that removal is
effectively controlled by jcr:removeNode.

=head2 Methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::AuthzUtil;
use Sling::Util;
use Sling::Print;
use Sling::Request;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return an Authz object.

=cut

sub new {
    my ( $class, $url, $lwpUserAgent ) = @_;
    die "url not defined!" unless defined $url;
    die "no lwp user agent provided!" unless defined $lwpUserAgent;
    my $response;
    my $content = { BaseURL => "$url",
                    LWP => $lwpUserAgent,
		    Message => "",
		    Response => \$response };
    bless( $content, $class );
    return $content;
}
#}}}

#{{{sub set_results

=pod

=head2 set_results

Populate the message and response with results returned from performing query:

=cut

sub set_results {
    my ( $content, $message, $response ) = @_;
    $content->{ 'Message' } = $message;
    $content->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub get_acl

=pod

=head2 get_acl

Return the access control list for the node in JSON format

=cut

sub get_acl {
    my ( $content, $remoteDest, $log ) = @_;
    my $res = ${ $content->{ 'LWP' } }->request( Sling::Request::string_to_request(
                  Sling::AuthzUtil::get_acl_setup( $content->{ 'BaseURL' }, $remoteDest ), $content->{ 'LWP' } ) );
    my $success = Sling::AuthzUtil::get_acl_eval( \$res );
    my $message = ( $success ? $res->content : "Could not view ACL for \"$remoteDest\"" );
    $content->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub grant_read

=pod

=head2 grant_privilege

Grant the privilege on a specified node.

=cut

sub grant_privilege {
    my ( $content, $remoteDest, $principal, $privilege, $log ) = @_;
    my $res = ${ $content->{ 'LWP' } }->request( Sling::Request::string_to_request(
                  Sling::AuthzUtil::grant_privilege_setup( $content->{ 'BaseURL' }, $remoteDest, $principal, "$privilege" ), $content->{ 'LWP' } ) );
    my $success = Sling::AuthzUtil::grant_privilege_eval( \$res );
    my $message = "Privilege \"$privilege\" on \"$remoteDest\" for \"$principal\" ";
    $message .= ( $success ? "granted." : "was not granted." );
    $content->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub deny_privilege

=pod

=head2 deny_privilege

Deny the privilege on a specified node.

=cut

sub deny_privilege {
    my ( $content, $remoteDest, $principal, $privilege, $log ) = @_;
    my $res = ${ $content->{ 'LWP' } }->request( Sling::Request::string_to_request(
                  Sling::AuthzUtil::deny_privilege_setup( $content->{ 'BaseURL' }, $remoteDest, $principal, "$privilege" ), $content->{ 'LWP' } ) );
    my $success = Sling::AuthzUtil::deny_privilege_eval( \$res );
    my $message = "Privilege \"$privilege\" on \"$remoteDest\" for \"$principal\" ";
    $message .= ( $success ? "denied." : "was not denied." );
    $content->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub grant_read

=pod

=head2 grant_read

Grant the read privilege on a specified node.

=cut

sub grant_read {
    my ( $content, $remoteDest, $principal, $log ) = @_;
    return $content->grant_privilege( $remoteDest, $principal, "read", $log );
}
#}}}

#{{{sub deny_read

=pod

=head2 deny_read

Deny the read privilege on a specified node.

=cut

sub deny_read {
    my ( $content, $remoteDest, $principal, $log ) = @_;
    return $content->deny_privilege( $remoteDest, $principal, "read", $log );
}
#}}}

#{{{sub grant_modifyProperties

=pod

=head2 grant_modifyProperties

Grant the modifyProperties privilege on a specified node.

=cut

sub grant_modifyProperties {
    my ( $content, $remoteDest, $principal, $log ) = @_;
    return $content->grant_privilege( $remoteDest, $principal, "modifyProperties", $log );
}
#}}}

#{{{sub deny_modifyProperties

=pod

=head2 deny_modifyProperties

Deny the modifyProperties privilege on a specified node.

=cut

sub deny_modifyProperties {
    my ( $content, $remoteDest, $principal, $log ) = @_;
    return $content->deny_privilege( $remoteDest, $principal, "modifyProperties", $log );
}
#}}}

#{{{sub delete

=pod

=head2 delete

Delete the access controls for a given principal on a given node:

=cut

sub delete {
    my ( $content, $remoteDest, $principal, $log ) = @_;
    my $res = ${ $content->{ 'LWP' } }->request( Sling::Request::string_to_request(
                  Sling::AuthzUtil::delete_setup( $content->{ 'BaseURL' }, $remoteDest, $principal ), $content->{ 'LWP' } ) );
    my $success = Sling::AuthzUtil::delete_eval( \$res );
    my $message = "Privileges on \"$remoteDest\" for \"$principal\" ";
    $message .= ( $success ? "removed." : "were not removed." );
    $content->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

1;
