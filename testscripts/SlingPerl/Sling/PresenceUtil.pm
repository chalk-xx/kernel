#!/usr/bin/perl

package Sling::PresenceUtil;

=head1 NAME

PresenceUtil - Utility library returning strings representing Rest queries that
perform presence operations in the system.

=head1 ABSTRACT

PresenceUtil perl library essentially provides the request strings needed to
interact with presence functionality exposed over the system rest interfaces.

Each interaction has a setup and eval method. setup provides the request,
whilst eval interprets the response to give further information about the
result of performing the request.

=cut

#{{{imports
use strict;
use lib qw ( .. );
#}}}

#{{{sub contacts_setup

=pod

=head2 contacts_setup

Returns a textual representation of the request needed to view the presence
status of the contacts for the logged in user in the system.

=cut

sub contacts_setup {
    my ( $baseURL ) = @_;
    die "No base URL provided to obtain contacts status for!" unless defined $baseURL;
    return "get $baseURL/~$user/presence.contacts.tidy.json";
}
#}}}

#{{{sub contacts_eval

=pod

=head2 contacts_eval

Check result of obtaining presence status of contacts.

=cut

sub contacts_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub delete_setup

=pod

=head2 delete_setup

Returns a textual representation of the request needed to clear the presence
status for the current user from the system.

=cut

sub delete_setup {
    my ( $baseURL ) = @_;
    die "No base URL provided to delete presence status for!" unless defined $baseURL;
    return "delete $baseURL/~$user/presence.json";
}
#}}}

#{{{sub delete_eval

=pod

=head2 delete_eval

Check result of obtaining presence delete.

=cut

sub delete_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^204$/ );
}
#}}}

#{{{sub status_setup

=pod

=head2 status_setup

Returns a textual representation of the request needed to fetch the presence
status for the current user from the system.

=cut

sub status_setup {
    my ( $baseURL ) = @_;
    die "No base URL provided to obtain status for!" unless defined $baseURL;
    return "get $baseURL/~$user/presence.tidy.json";
}
#}}}

#{{{sub status_eval

=pod

=head2 status_eval

Check result of obtaining presence status.

=cut

sub status_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub update_setup

=pod

=head2 update_setup

Returns a textual representation of the request needed to update the presence
status for the current user in the system.

=cut

sub update_setup {
    my ( $baseURL, $location, $status ) = @_;
    die "No base URL provided to obtain status for!" unless defined $baseURL;
    my $putVariables = "";
    $putVariables .= ( defined $location ? "sakai:location=$location&" : "" );
    $putVariables .= ( defined $status ? "sakai:status=$status&" : "" );
    $putVariables =~ s/&$//;
    return "put $baseURL/~$user/presence.json?$putVariables";
}
#}}}

#{{{sub update_eval

=pod

=head2 update_eval

Check result of performing presence update.

=cut

sub update_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^204$/ );
}
#}}}

1;
