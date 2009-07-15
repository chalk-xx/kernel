#!/usr/bin/perl

package Tests::Content;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Content;
use Test::More;
#}}}

#{{{sub run_regression_test

=pod

=head2 run_regression_test

Run regression tests for the content object.

=cut

sub run_regression_test {
    my ( $authn, $verbose, $log ) = @_;
    # test content name:
    my $test_content1 = "content_test_content_1_$$";
    my $test_content2 = "content_test_content_2_$$";
    my $test_content3 = "content_test_content_3_$$";
    # test properties:
    my @test_properties;
    # Sling content object:
    my $content = new Sling::Content( $authn, $verbose, $log );

    # Run tests:
    ok( defined $content,
        "Content Test: Sling Content Object successfully created." );
    ok( $content->add( $test_content1, \@test_properties ),
        "Content Test: Content \"$test_content1\" added successfully." );
    ok( $content->exists( $test_content1 ),
        "Content Test: Content \"$test_content1\" exists." );
    ok( ! $content->exists( "missing_$test_content1" ),
        "Content Test: Content \"missing_$test_content1\" should not exist." );

    # Check copying:
    ok( $content->copy( $test_content1, $test_content2 ),
        "Content Test: Content \"$test_content1\" copied to \"$test_content2\"." );
    ok( $content->exists( $test_content2 ),
        "Content Test: Content \"$test_content2\" exists." );
    ok( ! $content->copy( $test_content1, $test_content2 ),
        "Content Test: Can't copy content \"$test_content1\" to \"$test_content2\" without :replace." );
    ok( $content->copy( $test_content1, $test_content2, 1 ),
        "Content Test: Can copy content \"$test_content1\" to \"$test_content2\" with :replace." );
    ok( $content->exists( $test_content1 ),
        "Content Test: Content \"$test_content1\" exists." );
    ok( $content->exists( $test_content2 ),
        "Content Test: Content \"$test_content2\" exists." );

    # Check moving:
    ok( $content->move( $test_content2, $test_content3 ),
        "Content Test: Content \"$test_content2\" moved to \"$test_content3\"." );
    ok( $content->exists( $test_content3 ),
        "Content Test: Content \"$test_content3\" exists." );
    ok( ! $content->exists( $test_content2 ),
        "Content Test: Content \"$test_content2\" no longer exists." );
    ok( ! $content->move( $test_content1, $test_content3 ),
        "Content Test: Can't move content \"$test_content1\" to \"$test_content3\" without :replace." );
    ok( $content->move( $test_content1, $test_content3, 1 ),
        "Content Test: Can move content \"$test_content1\" to \"$test_content3\" with :replace." );
    ok( $content->exists( $test_content3 ),
        "Content Test: Content \"$test_content3\" exists." );
    ok( ! $content->exists( $test_content1 ),
        "Content Test: Content \"$test_content1\" no longer exists." );

    # Cleanup
    ok( $content->delete( $test_content3 ),
        "Content Test: Content \"$test_content3\" deleted successfully." );
    ok( ! $content->exists( $test_content3 ),
        "Content Test: Content \"$test_content3\" should no longer exist." );
}
#}}}

1;
