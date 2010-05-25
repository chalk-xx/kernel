#!/usr/bin/perl

package Sling::SearchUtil;

=head1 NAME

SearchUtil - Utility library returning strings representing Rest queries that
perform searches in the system.

=head1 ABSTRACT

SearchUtil perl library essentially provides the request strings needed to
interact with search functionality exposed over the system rest interfaces.

Each interaction has a setup and eval method. setup provides the request,
whilst eval interprets the response to give further information about the
result of performing the request.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::URL;
#}}}

#{{{sub search_setup

=pod

=head2 search_setup

Returns a textual representation of the request needed to search the system.

=cut

sub search_setup {
    my ( $baseURL, $searchTerm, $page, $items ) = @_;
    die "No base URL provided to search against!" unless defined $baseURL;
    die "No search term provided!" unless defined $searchTerm;
    $searchTerm = Sling::URL::urlencode( $searchTerm );
    my $specify_page = ( defined $page ? "&page=$page" : "" );
    my $specify_items = ( defined $items ? "&items=$items" : "" );
    return "get $baseURL/var/search/content.json?q=$searchTerm$specify_page$specify_items";
}
#}}}

#{{{sub search_eval

=pod

=head2 search_eval

Check result of system search.

=cut

sub search_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub search_sites_setup

=pod

=head2 search_sites_setup

Returns a textual representation of the request needed to search the system.

=cut

sub search_sites_setup {
    my ( $baseURL, $searchTerm, $page, $items ) = @_;
    die "No base URL provided to search against!" unless defined $baseURL;
    die "No search term provided!" unless defined $searchTerm;
    $searchTerm = Sling::URL::urlencode( $searchTerm );
    my $specify_page = ( defined $page ? "&page=$page" : "" );
    my $specify_items = ( defined $items ? "&items=$items" : "" );
    return "get $baseURL/var/search/sites.json?q=$searchTerm$specify_page$specify_items";
}
#}}}

#{{{sub search_sites_eval

=pod

=head2 search_sites_eval

Check result of system search.

=cut

sub search_sites_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub search_users_setup

=pod

=head2 search_users_setup

Returns a textual representation of the request needed to search the system.

=cut

sub search_users_setup {
    my ( $baseURL, $searchTerm, $page, $items ) = @_;
    die "No base URL provided to search against!" unless defined $baseURL;
    die "No search term provided!" unless defined $searchTerm;
    $searchTerm = Sling::URL::urlencode( $searchTerm );
    my $specify_page = ( defined $page ? "&page=$page" : "" );
    my $specify_items = ( defined $items ? "&items=$items" : "" );
    return "get $baseURL/var/search/users.json?username=$searchTerm$specify_page$specify_items";
}
#}}}

#{{{sub search_users_eval

=pod

=head2 search_users_eval

Check result of system search.

=cut

sub search_users_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

1;
