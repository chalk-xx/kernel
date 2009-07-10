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
    my $test_content = "content_test_content_$$";
    # test properties:
    my @test_properties;
    # Sling content object:
    my $content = new Sling::Content( $authn, $verbose, $log );

    # Run tests:
    ok( defined $content,
        "Content Test: Sling Content Object successfully created." );
    ok( $content->add( $test_content, \@test_properties ),
        "Content Test: Content \"$test_content\" added successfully." );
    ok( $content->exists( $test_content ),
        "Content Test: Content \"$test_content\" exists." );
    ok( ! $content->exists( "missing_$test_content" ),
        "Content Test: Content \"missing_$test_content\" should not exist." );
    ok( $content->delete( $test_content ),
        "Content Test: Content \"$test_content\" deleted successfully." );
    ok( ! $content->exists( $test_content ),
        "Content Test: Content \"$test_content\" should no longer exist." );
}
#}}}

1;
