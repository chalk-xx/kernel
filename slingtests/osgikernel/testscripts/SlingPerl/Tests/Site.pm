#!/usr/bin/perl

package Tests::Site;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Site;
use Test::More;
#}}}

#{{{sub run_regression_test

=pod

=head2 run_regression_test

Run regression tests for the site object.

=cut

sub run_regression_test {
    my ( $authn, $verbose, $log ) = @_;
    # test site name:
    my $test_site = "testing_site_$$";
    # test template:
    my $test_template;
    # test properties:
    my @test_properties;
    # test joinable
    my $test_joinable = "no";
    # Sling site object:
    my $site = new Sling::Site( $authn, $verbose, $log );

    # test user name:
    my $test_user = "testing_user_$$";
    # test user pass:
    my $test_pass = "pass";
    # Sling user object:
    my $user = new Sling::User( $authn, $verbose, $log );

    # Run tests:
    ok( defined $site,
        "Site Test: Sling Site Object successfully created." );
    ok( $site->update( $test_site, $test_template, $test_joinable, \@test_properties ),
        "Site Test: Site \"$test_site\" added successfully." );
    ok( $site->exists( $test_site ),
        "Site Test: Site \"$test_site\" exists." );

    # Create a user:
    ok( defined $user,
        "Site Test: Sling User Object successfully created." );
    ok( $user->add( $test_user, $test_pass, \@test_properties ),
        "Site Test: User \"$test_user\" added successfully." );
    ok( $user->exists( $test_user ),
        "Site Test: User \"$test_user\" exists." );

    # Test Site Membership:
    ok( $site->member_add( $test_site, $test_user ),
        "Site Test: Member \"$test_user\" added to \"$test_site\"." );
    ok( $site->member_exists( $test_site, $test_user ),
        "Site Test: Member \"$test_user\" exists in \"$test_site\"." );
    ok( $site->member_delete( $test_site, $test_user ),
        "Site Test: Member \"$test_user\" deleted from \"$test_site\"." );
    ok( ! $site->member_exists( $test_site, $test_user ),
        "Site Test: Member \"$test_user\" no longer exists in \"$test_site\"." );

    ok( $user->delete( $test_user, $test_pass, \@test_properties ),
        "Site Test: User \"$test_user\" added successfully." );
    ok( ! $user->exists( $test_user ),
        "Site Test: User \"$test_user\" no longer exists." );

    # Cleanup
    ok( $site->delete( $test_site ),
        "Site Test: Site \"$test_site\" deleted successfully." );
    ok( ! $site->exists( $test_site ),
        "Site Test: Site \"$test_site\" no longer exists." );
}
#}}}

1;
