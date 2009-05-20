#!/usr/bin/perl

package Sling::AuthzUtil;

=head1 NAME

AuthzUtil - Utility library returning strings representing queries that perform
authz operations in the system.

=head1 ABSTRACT

AuthzUtil perl library essentially provides the request strings needed to
interact with authz functionality exposed over the system interfaces.

Each interaction has a setup and eval method. setup provides the request,
whilst eval interprets the response to give further information about the
result of performing the request.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Util;
#}}}

#{{{sub get_acl_setup

=pod

=head2 get_acl_setup

Returns a textual representation of the request needed to retrieve the ACL for
a node in JSON format.

=cut

sub get_acl_setup {
    my ( $baseURL, $remoteDest ) = @_;
    die "No base url defined!" unless defined $baseURL;
    die "No destination to view ACL for defined!" unless defined $remoteDest;
    return "get $baseURL/$remoteDest.acl.json";
}
#}}}

#{{{sub get_acl_eval

=pod

=head2 get_acl_eval

Inspects the result returned from issuing the request generated in
get_acl_setup returning true if the result indicates the node ACL was returned
successfully, else false.

=cut

sub get_acl_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub delete_setup

=pod

=head2 delete_setup

Returns a textual representation of the request needed to retrieve the ACL for
a node in JSON format.

=cut

sub delete_setup {
    my ( $baseURL, $remoteDest, $principal ) = @_;
    die "No base url defined!" unless defined $baseURL;
    die "No destination to delete ACL for defined!" unless defined $remoteDest;
    die "No principal to delete ACL for defined!" unless defined $principal;
    my $postVariables = "\$postVariables = [':applyTo','$principal']";
    return "post $baseURL/$remoteDest.deleteAce.html $postVariables";
}
#}}}

#{{{sub delete_eval

=pod

=head2 delete_eval

Inspects the result returned from issuing the request generated in delete_setup
returning true if the result indicates the node ACL was deleted successfully,
else false.

=cut

sub delete_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub grant_privilege_setup

=pod

=head2 grant_privilege_setup

Returns a textual representation of the request needed to grant the privilege
privilege on a node.

=cut

sub grant_privilege_setup {
    my ( $baseURL, $remoteDest, $principal, $privilege ) = @_;
    die "No base url defined!" unless defined $baseURL;
    die "No destination to grant privilege for defined!" unless defined $remoteDest;
    die "No principal to grant privilege for defined!" unless defined $principal;
    my $postVariables = "\$postVariables = ['principalId','$principal','privilege\@jcr:$privilege','granted']";
    return "post $baseURL/$remoteDest.modifyAce.html $postVariables";
}
#}}}

#{{{sub grant_privilege_eval

=pod

=head2 grant_privilege_eval

Inspects the result returned from issuing the request generated in
grant_privilege_setup returning true if the result indicates the privilege was
granted successfully, else false.

=cut

sub grant_privilege_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub deny_privilege_setup

=pod

=head2 deny_privilege_setup

Returns a textual representation of the request needed to deny the 
privilege on a node.

=cut

sub deny_privilege_setup {
    my ( $baseURL, $remoteDest, $principal, $privilege ) = @_;
    die "No base url defined!" unless defined $baseURL;
    die "No destination to deny privilege for defined!" unless defined $remoteDest;
    die "No principal to deny privilege for defined!" unless defined $principal;
    my $postVariables = "\$postVariables = ['principalId','$principal','privilege\@jcr:$privilege','denied']";
    return "post $baseURL/$remoteDest.modifyAce.html $postVariables";
}
#}}}

#{{{sub deny_privilege_eval

=pod

=head2 deny_privilege_eval

Inspects the result returned from issuing the request generated in
deny_privilege_setup returning true if the result indicates the privilege was
denied successfully, else false.

=cut

sub deny_privilege_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

1;
