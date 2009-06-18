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
    my ( $baseURL, $searchTerm ) = @_;
    die "No base URL provided to search against!" unless defined $baseURL;
    die "No search term provided!" unless defined $searchTerm;
    $searchTerm = Sling::URL::urlencode( $searchTerm );
    return "get $baseURL/var/search/content.json?q=$searchTerm";
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

1;
