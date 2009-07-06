#!/usr/bin/perl

package Tests::Search;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Search;
use Test::More;
#}}}

#{{{sub run_regression_test

=pod

=head2 run_regression_test

Run regression tests for the search object.

=cut

sub run_regression_test {
    my ( $authn, $verbose, $log ) = @_;
    # test search word:
    my $test_search = "testing_search_$$";
    # test properties:
    my @test_properties = ( "search=$test_search" );
    # Sling user object:
    my $search = new Sling::Search( $authn, $verbose, $log );
    # Sling content object:
    my $content = new Sling::Content( $authn, $verbose, $log );
    # test content location:
    my $test_content = "testing_content_$$";

    # Run tests:
    ok( defined $search,
        "Search Test: Sling Search Object successfully created." );
    ok( defined $content,
        "Search Test: Sling Content Object successfully created." );

    # Add content:
    ok( $content->add( $test_content, \@test_properties ),
        "Search Test: Content \"$test_content\" added successfully." );
    ok( $content->exists( $test_content ),
        "Search Test: Content \"$test_content\" exists." );

    # Perform searches:
    ok( $search->search( $test_search ) == 1,
        "Search Test: Search \"$test_search\" matched 1 item." );
    ok( $search->search( "missing_" . $test_search . "_missing" ) == 0,
        "Search Test: Search  \"missing_" . "$test_search" . "_missing\" matched 0 items." );

    # Delete content:
    ok( $content->delete( $test_content ),
        "Search Test: Content \"$test_content\" deleted successfully." );
    ok( ! $content->exists( $test_content ),
        "Search Test: Content \"$test_content\" should no longer exist." );
}
#}}}

1;
