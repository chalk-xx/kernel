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

#{{{sub member_add_setup

=pod

=head2 member_add_setup

Returns a textual representation of the request needed to add a member to a
site in the system.

=cut

sub member_add_setup {
    my ( $baseURL, $actOnSite, $addMember ) = @_;
    die "No base url defined to add against!" unless defined $baseURL;
    die "No site name defined to add member to!" unless defined $actOnSite;
    die "No member name defined to add!" unless defined $addMember;
    my $postVariables = "\$postVariables = ['addauth','$addMember']";
    return "post $baseURL/$actOnSite.authorize.json $postVariables";
}
#}}}

#{{{sub member_add_eval

=pod

=head2 member_add_eval

Check addition of member to site in the system.

=cut

sub member_add_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#

=pod

=head2 member_delete_setup

Returns a textual representation of the request needed to delete a member from a
site in the system.

=cut

sub member_delete_setup {
    my ( $baseURL, $actOnSite, $deleteMember ) = @_;
    die "No base url defined to delete against!" unless defined $baseURL;
    die "No site name defined to delete member from!" unless defined $actOnSite;
    die "No member name defined to delete!" unless defined $deleteMember;
    my $postVariables = "\$postVariables = ['removeauth','$deleteMember']";
    return "post $baseURL/$actOnSite.authorize.json $postVariables";
}
#}}}

#{{{sub member_delete_eval

=pod

=head2 member_delete_eval

Check removal of member of site in the system.

=cut

sub member_delete_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub member_view_setup

=pod

=head2 member_view_setup

Returns a textual representation of the request needed to return a list of
members for a given site.

=cut

sub member_view_setup {
    my ( $baseURL, $id ) = @_;
    die "No base url defined to check existence against!" unless defined $baseURL;
    die "No site id to check existence of defined!" unless defined $id;
    return "get $baseURL/$id.members.json";
}
#}}}

#{{{sub member_view_eval

=pod

=head2 member_view_eval

Inspects the result returned from issuing the request generated in exists_setup
returning true if the result indicates the site does exist in the system, else
false.

=cut

sub member_view_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

1;
