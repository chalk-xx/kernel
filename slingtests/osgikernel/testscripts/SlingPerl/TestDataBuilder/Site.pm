#!/usr/bin/perl

package TestDataBuilder::Site;

=head1 NAME

Site - Generate sample test data for use in test runs.

=head1 ABSTRACT

Generate suitable test data for testing Site functionality.

=cut

#{{{imports
use warnings;
use strict;
use Carp;

#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Content object.

=cut

sub new {
    my ( $class, $testDataDirectory, $verbose, $log ) = @_;
    my $site = {
        BaseDir => $testDataDirectory,
        Verbose => $verbose,
        Log     => $log
    };
    bless( $site, $class );
    return $site;
}

#}}}

#{{{sub generate

=pod

=head2 generate

Generate site test data

=cut

sub generate {
    my ($site) = @_;
    Sling::Print::print_with_lock( "Creating site test data.", $site->{'Log'} );
    my $site_data_file = $site->{'BaseDir'} . "/site_additions.txt";
    if ( -f $site_data_file ) {
        my $success = unlink($site_data_file);
        croak "Could not clear existing site data file" unless $success;
    }
    Sling::Print::print_with_lock( "\"site\",\"sakai:joinable\"",
        $site_data_file );

    for ( my $i = 1 ; $i <= 4000 ; $i++ ) {
        Sling::Print::print_with_lock( "\"testsite$i\",\"yes\"",
            $site_data_file );
    }

    Sling::Print::print_with_lock( "Site test data created.", $site->{'Log'} );
    return 1;
}

#}}}

1;
