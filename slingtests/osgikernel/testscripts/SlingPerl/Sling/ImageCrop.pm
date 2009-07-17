#!/usr/bin/perl

package Sling::ImageCrop;

=head1 NAME

ImageCrop - image crop related functionality for Sakai implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST image crop methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use JSON;
use Sling::Print;
use Sling::Request;
use Sling::ImageCropUtil;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return an ImageCrop object.

=cut

sub new {
    my ( $class, $authn, $verbose, $log ) = @_;
    die "no authn provided!" unless defined $authn;
    my $response;
    my $image_crop = { BaseURL => $$authn->{ 'BaseURL' },
                       Authn => $authn,
		       Message => "",
		       Response => \$response,
		       Verbose => $verbose,
		       Log => $log };
    bless( $image_crop, $class );
    return $image_crop;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $image_crop, $message, $response ) = @_;
    $image_crop->{ 'Message' } = $message;
    $image_crop->{ 'Response' } = $response;
    return 1;
}
#}}}

1;
