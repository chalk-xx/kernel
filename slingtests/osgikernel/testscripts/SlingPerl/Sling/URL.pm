#!/usr/bin/perl

package Sling::URL;

=head1 NAME

URL - useful utility functions for manipulating URLs.

=head1 ABSTRACT

Utility library providing useful URL functions for general Rest functionality.

=cut

#{{{imports
use strict;
use lib qw ( .. );
#}}}

#{{{sub urlencode

=pod

=head2 urlencode

Function to encode a string so it is suitable for use in urls.

=cut

sub urlencode {
    my ( $value ) = @_;
    $value =~ s/([^a-zA-Z_0-9 ])/"%" . uc(sprintf "%lx" , unpack("C", $1))/eg;
    $value =~ tr/ /+/;
    return ($value);
}
#}}}

#{{{sub url_to_realm

=pod

=head2 url_to_realm

Function to convert an url to a realm - Strips away the http or https and any
query string or trailing path. Adds a port number definition.

=cut

sub url_to_realm {
    my ( $url ) = @_;
    my $realm = $url;
    # Strip any query string:
    $realm =~ s/(.*)\?.*?$/$1/;
    # Strip everything after a first slash - including the slash:
    $realm =~ s#(https?://|)([^/]*).*#$1$2#;
    # Test if port is defined:
    if ( $realm !~ /:[0-9]+$/ ) {
        # No port specified yet, need to add one:
        $realm = ( $realm =~ /^http:/ ? "$realm:80" : "$realm:443" );
    }
    # Strip the protocol for the realm:
    $realm =~ s#https?://(.*)#$1#;
    return $realm;
}
#}}}

1;
