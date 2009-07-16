#!/usr/bin/perl

package Tests::Group;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Group;
use Sling::User;
use Test::More;
#}}}

#{{{sub run_regression_test

=pod

=head2 run_regression_test

Run regression tests for the group object.

=cut

sub run_regression_test {
    my ( $authn, $verbose, $log ) = @_;
    # test group name:
    my $test_group1 = "g-group_test_group_1_$$";
    my $test_group2 = "g-group_test_group_2_$$";
    # test properties:
    my @test_properties;
    # Sling group object:
    my $group = new Sling::Group( $authn, $verbose, $log );

    # test user name:
    my $test_user = "group_test_user_$$";
    # test user pass:
    my $test_pass = "pass";
    # Sling user object:
    my $user = new Sling::User( $authn, $verbose, $log );

    # Run tests:
    ok( defined $group,
        "Group Test: Sling Group Object successfully created." );
    ok( defined $user,
        "Group Test: Sling User Object successfully created." );
    # create groups:
    ok( $group->add( $test_group1, \@test_properties ),
        "Group Test: Group \"$test_group1\" added successfully." );
    ok( $group->exists( $test_group1 ),
        "Group Test: Group \"$test_group1\" exists." );
    ok( $group->add( $test_group2, \@test_properties ),
        "Group Test: Group \"$test_group2\" added successfully." );
    ok( $group->exists( $test_group2 ),
        "Group Test: Group \"$test_group2\" exists." );

    # Attempt to create bad group:
    ok( ! $group->add( "badgroupname", \@test_properties ),
        "Group Test: Group \"badgroupname\" (no g- at start) creation denied." );
    ok( ! $group->exists( "badgroupname" ),
        "Group Test: Group \"badgroupname\" does not exist." );

    # Add test user:
    ok( $user->add( $test_user, $test_pass, \@test_properties ),
        "Group Test: User \"$test_user\" added successfully." );
    ok( $user->exists( $test_user ),
        "Group Test: User \"$test_user\" exists." );
    
    # Test Group Membership:
    ok( $group->member_add( $test_group1, $test_user ),
        "Group Test: Member \"$test_user\" added to \"$test_group1\"." );
    ok( $group->member_exists( $test_group1, $test_user ),
        "Group Test: Member \"$test_user\" exists in \"$test_group1\"." );
    ok( $group->member_view( $test_group1 ) == 1,
        "Group Test: 1 Member in \"$test_group1\"." );

    ok( $group->member_add( $test_group2, $test_user ),
        "Group Test: Member \"$test_user\" added to \"$test_group2\"." );
    ok( $group->member_exists( $test_group2, $test_user ),
        "Group Test: Member \"$test_user\" exists in \"$test_group2\"." );
    ok( $group->member_view( $test_group2 ) == 1,
        "Group Test: 1 Member in \"$test_group2\"." );

    ok( $group->member_add( $test_group1, $test_group2 ),
        "Group Test: Member \"$test_group2\" added to \"$test_group1\"." );
    ok( $group->member_exists( $test_group1, $test_group2 ),
        "Group Test: Member \"$test_group2\" exists in \"$test_group1\"." );
    ok( $group->member_view( $test_group1 ) == 2,
        "Group Test: 2 Members in \"$test_group1\"." );

    ok( ! $group->member_add( $test_group2, $test_group1 ),
        "Group Test: Member \"$test_group1\" should not be added to \"$test_group2\"." );
    ok( ! $group->member_exists( $test_group2, $test_group1 ),
        "Group Test: Member \"$test_group1\" should not exist in \"$test_group2\"." );
    ok( $group->member_view( $test_group2 ) == 1,
        "Group Test: Still 1 Member in \"$test_group2\"." );

    # Delete members from groups:
    ok( $group->member_delete( $test_group1, $test_user ),
        "Group Test: Member \"$test_user\" deleted from \"$test_group1\"." );
    ok( $group->member_exists( $test_group1, $test_user ),
        "Group Test: Member \"$test_user\" should still exist in \"$test_group1\"." );
    ok( $group->member_view( $test_group1 ) == 2,
        "Group Test: 1 Member in \"$test_group1\"." );

    ok( $group->member_delete( $test_group1, $test_group2 ),
        "Group Test: Member \"$test_user\" deleted from \"$test_group1\"." );
    ok( ! $group->member_exists( $test_group1, $test_user ),
        "Group Test: Member \"$test_user\" no longer exists in \"$test_group1\"." );
    ok( ! $group->member_exists( $test_group1, $test_group2 ),
        "Group Test: Member \"$test_group2\" no longer exists in \"$test_group1\"." );
    ok( $group->member_view( $test_group1 ) == 0,
        "Group Test: 0 Members in \"$test_group1\"." );

    ok( $group->member_delete( $test_group2, $test_user ),
        "Group Test: Member \"$test_user\" deleted from \"$test_group1\"." );
    ok( ! $group->member_exists( $test_group2, $test_user ),
        "Group Test: Member \"$test_user\" no longer exists in \"$test_group2\"." );
    ok( $group->member_view( $test_group2 ) == 0,
        "Group Test: 0 Members in \"$test_group2\"." );

    # Cleanup Users:
    ok( $user->delete( $test_user ),
        "Group Test: User \"$test_user\" deleted successfully." );
    ok( ! $user->exists( $test_user ),
        "Group Test: User \"$test_user\" no longer exists." );
    
    # Cleanup Groups:
    ok( $group->delete( $test_group1 ),
        "Group Test: Group \"$test_group1\" deleted successfully." );
    ok( ! $group->exists( $test_group1 ),
        "Group Test: Group \"$test_group1\" should no longer exist." );
    ok( $group->delete( $test_group2 ),
        "Group Test: Group \"$test_group2\" deleted successfully." );
    ok( ! $group->exists( $test_group2 ),
        "Group Test: Group \"$test_group2\" should no longer exist." );
}
#}}}

1;
