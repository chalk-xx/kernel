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
    my $test_site = "site_test_site_$$";
    # test template:
    my $test_template;
    # test properties:
    my @test_properties;
    # test joinable
    my $test_joinable = "no";
    # Sling site object:
    my $site = new Sling::Site( $authn, $verbose, $log );

    # test user name:
    my $test_user1 = "site_test_user_1_$$";
    my $test_user2 = "site_test_user_2_$$";
    my $test_user3 = "site_test_user_3_$$";
    # test user pass:
    my $test_pass = "pass";
    # Sling user object:
    my $user = new Sling::User( $authn, $verbose, $log );

    # test group name:
    my $test_group1 = "g-site_test_group_1_$$";
    my $test_group2 = "g-site_test_group_2_$$";
    # Sling group object:
    my $group = new Sling::Group( $authn, $verbose, $log );

    # Run tests:
    ok( defined $site,
        "Site Test: Sling Site Object successfully created." );
    ok( $site->update( $test_site, $test_template, $test_joinable, \@test_properties ),
        "Site Test: Site \"$test_site\" added successfully." );
    ok( $site->exists( $test_site ),
        "Site Test: Site \"$test_site\" exists." );

    # Create users:
    ok( defined $user,
        "Site Test: Sling User Object successfully created." );
    ok( $user->add( $test_user1, $test_pass, \@test_properties ),
        "Site Test: User \"$test_user1\" added successfully." );
    ok( $user->exists( $test_user1 ),
        "Site Test: User \"$test_user1\" exists." );
    ok( $user->add( $test_user2, $test_pass, \@test_properties ),
        "Site Test: User \"$test_user2\" added successfully." );
    ok( $user->exists( $test_user2 ),
        "Site Test: User \"$test_user2\" exists." );
    ok( $user->add( $test_user3, $test_pass, \@test_properties ),
        "Site Test: User \"$test_user3\" added successfully." );
    ok( $user->exists( $test_user3 ),
        "Site Test: User \"$test_user3\" exists." );

    # create groups:
    ok( defined $group,
        "Site Test: Sling Group Object successfully created." );
    ok( $group->add( $test_group1, \@test_properties ),
        "Site Test: Group \"$test_group1\" added successfully." );
    ok( $group->exists( $test_group1 ),
        "Site Test: Group \"$test_group1\" exists." );
    ok( $group->add( $test_group2, \@test_properties ),
        "Site Test: Group \"$test_group2\" added successfully." );
    ok( $group->exists( $test_group2 ),
        "Site Test: Group \"$test_group2\" exists." );

    # Add users to groups:
    ok( $group->member_add( $test_group1, $test_user1 ),
        "Site Test: Member \"$test_user1\" added to \"$test_group1\"." );
    ok( $group->member_exists( $test_group1, $test_user1 ),
        "Site Test: Member \"$test_user1\" exists in \"$test_group1\"." );
    ok( $group->member_add( $test_group2, $test_user1 ),
        "Site Test: Member \"$test_user1\" added to \"$test_group2\"." );
    ok( $group->member_exists( $test_group2, $test_user1 ),
        "Site Test: Member \"$test_user1\" exists in \"$test_group2\"." );
    ok( $group->member_add( $test_group2, $test_user2 ),
        "Site Test: Member \"$test_user2\" added to \"$test_group2\"." );
    ok( $group->member_exists( $test_group2, $test_user2 ),
        "Site Test: Member \"$test_user2\" exists in \"$test_group2\"." );

    # Test Site Membership:
    ok( $site->member_add( $test_site, $test_group1 ),
        "Site Test: Group \"$test_group1\" added to \"$test_site\"." );
    ok( $site->member_exists( $test_site, $test_user1 ),
        "Site Test: Member \"$test_user1\" now exists in \"$test_site\"." );
    ok( $site->member_view( $test_site ) == 1,
        "Site Test: 1 member now exists in \"$test_site\"." );
    ok( $site->member_add( $test_site, $test_group2 ),
        "Site Test: Group \"$test_group2\" added to \"$test_site\"." );
    ok( $site->member_exists( $test_site, $test_user1 ),
        "Site Test: Member \"$test_user1\" still exists in \"$test_site\"." );
    ok( $site->member_exists( $test_site, $test_user2 ),
        "Site Test: Member \"$test_user2\" now exists in \"$test_site\"." );
    ok( $site->member_view( $test_site ) == 2,
        "Site Test: 2 members now exist in \"$test_site\"." );
    ok( $site->member_add( $test_site, $test_user3 ),
        "Site Test: User \"$test_user3\" added to \"$test_site\"." );
    ok( $site->member_exists( $test_site, $test_user1 ),
        "Site Test: Member \"$test_user1\" still exists in \"$test_site\"." );
    ok( $site->member_exists( $test_site, $test_user2 ),
        "Site Test: Member \"$test_user2\" still exists in \"$test_site\"." );
    ok( $site->member_exists( $test_site, $test_user3 ),
        "Site Test: Member \"$test_user3\" now exists in \"$test_site\"." );
    ok( $site->member_view( $test_site ) == 3,
        "Site Test: 3 members now exist in \"$test_site\"." );

    # Remove test_group1 from site
    ok( $site->member_delete( $test_site, $test_group1 ),
        "Site Test: Group \"$test_group1\" deleted from \"$test_site\"." );
    ok( $site->member_exists( $test_site, $test_user1 ),
        "Site Test: Member \"$test_user1\" still exists in \"$test_site\"." );
    ok( $site->member_exists( $test_site, $test_user2 ),
        "Site Test: Member \"$test_user2\" still exists in \"$test_site\"." );
    ok( $site->member_exists( $test_site, $test_user3 ),
        "Site Test: Member \"$test_user3\" still exists in \"$test_site\"." );
    ok( $site->member_view( $test_site ) == 3,
        "Site Test: 3 members now exist in \"$test_site\"." );

    # Remove test_group2 from site
    ok( $site->member_delete( $test_site, $test_group2 ),
        "Site Test: Group \"$test_group2\" deleted from \"$test_site\"." );
    ok( ! $site->member_exists( $test_site, $test_user1 ),
        "Site Test: Member \"$test_user1\" no longer exists in \"$test_site\"." );
    ok( ! $site->member_exists( $test_site, $test_user2 ),
        "Site Test: Member \"$test_user2\" no longer exists in \"$test_site\"." );
    ok( $site->member_exists( $test_site, $test_user3 ),
        "Site Test: Member \"$test_user3\" still exists in \"$test_site\"." );
    ok( $site->member_view( $test_site ) == 1,
        "Site Test: 1 member now exists in \"$test_site\"." );

    # Remove test_user3 from site
    ok( $site->member_delete( $test_site, $test_user3 ),
        "Site Test: User \"$test_user3\" deleted from \"$test_site\"." );
    ok( ! $site->member_exists( $test_site, $test_user1 ),
        "Site Test: Member \"$test_user1\" no longer exists in \"$test_site\"." );
    ok( ! $site->member_exists( $test_site, $test_user2 ),
        "Site Test: Member \"$test_user2\" no longer exists in \"$test_site\"." );
    ok( ! $site->member_exists( $test_site, $test_user3 ),
        "Site Test: Member \"$test_user3\" no longer exists in \"$test_site\"." );
    ok( $site->member_view( $test_site ) == 0,
        "Site Test: 0 members now exist in \"$test_site\"." );

    # Delete created groups:
    ok( $group->delete( $test_group1 ),
        "Site Test: Group \"$test_group1\" deleted successfully." );
    ok( ! $group->exists( $test_group1 ),
        "Site Test: Group \"$test_group1\" no longer exists." );
    ok( $group->delete( $test_group2 ),
        "Site Test: Group \"$test_group2\" deleted successfully." );
    ok( $group->exists( $test_group2 ),
        "Site Test: Group \"$test_group2\" no longer exists." );

    # Delete created users:
    ok( $user->delete( $test_user1 ),
        "Site Test: User \"$test_user1\" deleted successfully." );
    ok( ! $user->exists( $test_user1 ),
        "Site Test: User \"$test_user1\" no longer exists." );
    ok( $user->delete( $test_user2 ),
        "Site Test: User \"$test_user2\" deleted successfully." );
    ok( ! $user->exists( $test_user2 ),
        "Site Test: User \"$test_user2\" no longer exists." );

    # Delete created sites:
    ok( $site->delete( $test_site ),
        "Site Test: Site \"$test_site\" deleted successfully." );
    ok( ! $site->exists( $test_site ),
        "Site Test: Site \"$test_site\" no longer exists." );
}
#}}}

1;
