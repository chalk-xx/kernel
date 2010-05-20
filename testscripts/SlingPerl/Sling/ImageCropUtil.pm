#!/usr/bin/perl

package Sling::ImageCropUtil;

=head1 NAME

ImageCropUtil - Utility library returning strings representing Rest queries
that perform image cropping and scaling in the system.

=head1 ABSTRACT

ImageCropUtil perl library essentially provides the request strings needed to
interact with image crop functionality exposed over the system rest interfaces.

Each interaction has a setup and eval method. setup provides the request,
whilst eval interprets the response to give further information about the
result of performing the request.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::URL;
#}}}

1;
