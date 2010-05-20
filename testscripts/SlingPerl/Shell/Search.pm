#!/usr/bin/perl

package Shell::Search;

=head1 NAME

An administrative terminal for Sling, allowing interactive administration of the Sling system.

=head1 ABSTRACT

An administrative terminal for Sling, allowing interactive administration of the Sling system.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use vars qw(@ISA @EXPORT);
use Sling::Search;
#}}}

@ISA = qw(Exporter);

require Exporter;

@EXPORT = qw(help_search smry_search);

#{{{sub run_search
sub run_search {
    my ( $term, $searchTerm, $config ) = @_;
    if ( ! defined $searchTerm ) {
        $searchTerm = $term->prompt("Please enter a search term: ");
    }
    if ( $searchTerm =~ /^\s*$/ ) {
        return 1;
    }
    my $search = new Sling::Search( $config->{ 'host' }, $config->{ 'lwp' } );
    $search->search( $searchTerm );
    print $search->{ 'Message' } . "\n";
    return 1;
}
#}}}

#{{{sub help_search
sub help_search {
    return "Specify a search term to search for that term within the system.";
}
#}}}

#{{{sub smry_search
sub smry_search {
    return "Search for a term within the system";
}
#}}}

1;
