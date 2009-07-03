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
    my ( $auth, $log, $verbose ) = @_;
    # test group name:
    my $test_group = "g-test_group_$$";
    # test properties:
    my @test_properties;
    # Sling group object:
    my $group = new Sling::Group( $auth, $verbose, $log );

    # test user name:
    my $test_user = "testing_user_$$";
    # test user pass:
    my $test_pass = "pass";
    # Sling user object:
    my $user = new Sling::User( $auth, $verbose, $log );

    # Run tests:
    ok( defined $group,
        "Group Test: Sling Group Object successfully created." );
    ok( defined $user,
        "Group Test: Sling User Object successfully created." );
    ok( $group->add( $test_group, \@test_properties, $log ),
        "Group Test: Group \"$test_group\" added successfully." );
    ok( $group->exists( $test_group, $log ),
        "Group Test: Group \"$test_group\" exists." );
    ok( ! $group->add( "badgroupname", \@test_properties, $log ),
        "Group Test: Group \"badgroupname\" (no g- at start) creation denied." );
    ok( ! $group->exists( "badgroupname", $log ),
        "Group Test: Group \"badgroupname\" does not exist." );
    
    # Test Group Membership:
    ok( $user->add( $test_user, $test_pass, \@test_properties, $log ),
        "Group Test: User \"$test_user\" added successfully." );
    ok( $group->member_add( $test_group, $test_user, $log ),
        "Group Test: Member \"$test_user\" added to \"$test_group\"." );
    ok( $group->member_exists( $test_group, $test_user, $log ),
        "Group Test: Member \"$test_user\" exists in \"$test_group\"." );
    ok( $group->member_delete( $test_group, $test_user, $log ),
        "Group Test: Member \"$test_user\" deleted from \"$test_group\"." );
    ok( ! $group->member_exists( $test_group, $test_user, $log ),
        "Group Test: Member \"$test_user\" no longer exists in \"$test_group\"." );
    ok( $user->delete( $test_user, $log ),
        "Group Test: User \"$test_user\" deleted successfully." );
    
    # Cleanup
    ok( $group->delete( $test_group, $log ),
        "Group Test: Group \"$test_group\" deleted successfully." );
}
#}}}

1;
