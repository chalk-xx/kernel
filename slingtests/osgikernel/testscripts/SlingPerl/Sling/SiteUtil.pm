#!/usr/bin/perl

package Sling::SiteUtil;

=head1 NAME

SiteUtil - Utility library returning strings representing Rest queries that
perform site related actions in the system.

=head1 ABSTRACT

SiteUtil perl library essentially provides the request strings needed to
interact with site functionality exposed over the system rest interfaces.

Each interaction has a setup and eval method. setup provides the request,
whilst eval interprets the response to give further information about the
result of performing the request.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::URL;
#}}}

#{{{sub add_member_setup

=pod

=head2 add_member_setup

Returns a textual representation of the request needed to add a member to a
site in the system.

=cut

sub add_member_setup {
    my ( $baseURL, $id, $member, $role ) = @_;
    die "No base url defined to add against!" unless defined $baseURL;
    die "No site id defined to add owner to!" unless defined $id;
    die "No member to add defined for id $id!" unless defined $member;
    die "No role to apply to added member $member defined for id $id!" unless defined $role;
    $id = Sling::URL::urlencode( $id );
    $member = Sling::URL::urlencode( $member );
    $role = Sling::URL::urlencode( $role );

    my $postVariables = "\$postVariables = ['uuserid','$member','membertoken','$role']";
    return "post $baseURL/_rest/site/members/add/$id $postVariables";
}
#}}}

#{{{sub add_member_eval

=pod

=head2 add_member_eval

Check addition of member to site in the system.

=cut

sub add_member_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub list_members_setup

=pod

=head2 list_members_setup

Returns a textual representation of the request needed to test whether a given
site exists in the system.

=cut

sub list_members_setup {
    my ( $baseURL, $id ) = @_;
    die "No base url defined to check existence against!" unless defined $baseURL;
    die "No site id to check existence of defined!" unless defined $id;
    $id = Sling::URL::urlencode( $id );
    return "get $baseURL/_rest/sites/list/$id";
}
#}}}

#{{{sub list_members_eval

=pod

=head2 list_members_eval

Inspects the result returned from issuing the request generated in exists_setup
returning true if the result indicates the site does exist in the system, else
false.

=cut

sub list_members_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub remove_member_setup

=pod

=head2 remove_member_setup

Returns a textual representation of the request needed to remove a member from
a site in the system.

=cut

sub remove_member_setup {
    my ( $baseURL, $id, $member, $role ) = @_;
    die "No base url defined to remove against!" unless defined $baseURL;
    die "No site id defined to remove owner from!" unless defined $id;
    die "No member to remove defined for id $id!" unless defined $member;
    die "No role defined for member $member being removed for id $id!" unless defined $role;
    $id = Sling::URL::urlencode( $id );
    $member = Sling::URL::urlencode( $member );
    $role = Sling::URL::urlencode( $role );

    my $postVariables = "\$postVariables = ['uuserid','$member','membertoken','$role']";
    return "post $baseURL/_rest/site/members/remove/$id $postVariables";
}
#}}}

#{{{sub remove_member_eval

=pod

=head2 remove_member_eval

Check removal of member of site in the system.

=cut

sub remove_member_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

1;
