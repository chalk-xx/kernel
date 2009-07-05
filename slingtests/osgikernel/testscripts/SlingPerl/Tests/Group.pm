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
    my $test_group = "g-test_group_$$";
    # test properties:
    my @test_properties;
    # Sling group object:
    my $group = new Sling::Group( $authn, $verbose, $log );

    # test user name:
    my $test_user = "testing_user_$$";
    # test user pass:
    my $test_pass = "pass";
    # Sling user object:
    my $user = new Sling::User( $authn, $verbose, $log );

    # Run tests:
    ok( defined $group,
        "Group Test: Sling Group Object successfully created." );
    ok( defined $user,
        "Group Test: Sling User Object successfully created." );
    ok( $group->add( $test_group, \@test_properties ),
        "Group Test: Group \"$test_group\" added successfully." );
    ok( $group->exists( $test_group ),
        "Group Test: Group \"$test_group\" exists." );
    ok( ! $group->add( "badgroupname", \@test_properties ),
        "Group Test: Group \"badgroupname\" (no g- at start) creation denied." );
    ok( ! $group->exists( "badgroupname" ),
        "Group Test: Group \"badgroupname\" does not exist." );
    
    # Test Group Membership:
    ok( $user->add( $test_user, $test_pass, \@test_properties ),
        "Group Test: User \"$test_user\" added successfully." );
    ok( $group->member_add( $test_group, $test_user ),
        "Group Test: Member \"$test_user\" added to \"$test_group\"." );
    ok( $group->member_exists( $test_group, $test_user ),
        "Group Test: Member \"$test_user\" exists in \"$test_group\"." );
    ok( $group->member_delete( $test_group, $test_user ),
        "Group Test: Member \"$test_user\" deleted from \"$test_group\"." );
    ok( ! $group->member_exists( $test_group, $test_user ),
        "Group Test: Member \"$test_user\" no longer exists in \"$test_group\"." );

    ok( $user->delete( $test_user ),
        "Group Test: User \"$test_user\" deleted successfully." );
    ok( ! $user->exists( $test_user ),
        "Group Test: User \"$test_user\" no longer exists." );
    
    # Cleanup
    ok( $group->delete( $test_group ),
        "Group Test: Group \"$test_group\" deleted successfully." );
    ok( ! $group->exists( $test_group ),
        "Group Test: Group \"$test_group\" should no longer exist." );
}
#}}}

1;
