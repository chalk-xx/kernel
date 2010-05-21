#!/usr/bin/perl

package Tests::Search;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Search;
use Sling::User;
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
    my $test_search = "search_test_search_$$";
    # test properties:
    my @test_properties = ( "search=$test_search" );
    # Sling user object:
    my $search = new Sling::Search( $authn, $verbose, $log );
    # Sling content object:
    my $content = new Sling::Content( $authn, $verbose, $log );
    # test content location:
    my $test_content = "search_test_content_$$";

    # test user name:
    my $test_user = "search_test_user_$$";
    # test user pass:
    my $test_pass = "pass";
    # Sling user object:
    my $user = new Sling::User( $authn, $verbose, $log );

    # test site name:
    my $test_site = "search_test_site_$$";
    # test template:
    my $test_template;
    # test joinable
    my $test_joinable = "no";
    # Sling site object:
    my $site = new Sling::Site( $authn, $verbose, $log );

    # Run tests:
    ok( defined $search,
        "Search Test: Sling Search Object successfully created." );
    ok( defined $content,
        "Search Test: Sling Content Object successfully created." );
    ok( defined $user,
        "Search Test: Sling User Object successfully created." );
    ok( defined $site,
        "Search Test: Sling Site Object successfully created." );

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
    ok( $search->search( $test_search ) == 0,
        "Search Test: Search \"$test_search\" matched 0 items." );

    # Add user:
    ok( $user->add( $test_user, $test_pass, \@test_properties ),
        "Search Test: User \"$test_user\" added successfully." );
    ok( $user->exists( $test_user ),
        "Search Test: User \"$test_user\" exists." );

    # Perform searches:
    ok( $search->search_users( $test_user ) == 1,
        "Search Test: Search \"$test_user\" matched 1 user." );
    ok( $search->search_users( "missing_" . $test_user . "_missing" ) == 0,
        "Search Test: Search  \"missing_" . "$test_user" . "_missing\" matched 0 users." );

    # Delete user:
    ok( $user->delete( $test_user ),
        "Search Test: User \"$test_user\" deleted successfully." );
    ok( ! $user->exists( $test_user ),
        "Search Test: User \"$test_user\" should no longer exist." );

    # Add site:
    ok( $site->update( $test_site, $test_template, $test_joinable, \@test_properties ),
        "Search Test: Site \"$test_site\" added successfully." );
    ok( $site->exists( $test_site ),
        "Search Test: Site \"$test_site\" exists." );

    # Perform searches:
    ok( $search->search_sites( $test_search ) == 1,
        "Search Test: Search \"$test_search\" matched 1 site." );
    ok( $search->search_sites( "missing_" . $test_search . "_missing" ) == 0,
        "Search Test: Search  \"missing_" . "$test_search" . "_missing\" matched 0 sites." );

    # Delete site:
    ok( $site->delete( $test_site ),
        "Search Test: Site \"$test_site\" deleted successfully." );
    ok( ! $site->exists( $test_site ),
        "Search Test: Site \"$test_site\" deleted." );
    ok( $search->search_sites( $test_search ) == 0,
        "Search Test: Search \"$test_search\" matched 0 sites." );
}
#}}}

1;
