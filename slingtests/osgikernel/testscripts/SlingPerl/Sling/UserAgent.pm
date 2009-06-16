#!/usr/bin/perl

package Sling::UserAgent;

=head1 NAME

UserAgent - useful utility functions for general LWP User agent functionality.

=head1 ABSTRACT

Utility library providing useful utility functions for general LWP User Agent functionality.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use LWP::UserAgent ();
use Sling::Authn;
use Sling::URL;
use Sling::Print;
#}}}

#{{{sub get_user_agent

=pod

=head2 get_user_agent

Returns a user agent object, set up to follow redirects on POST and with a
suitable cookie container.

=cut

sub get_user_agent {
    # Optional parameters if auth is desired:
    my ( $log, $url, $username, $password, $type ) = @_;
    my $lwpUserAgent = LWP::UserAgent->new( keep_alive=>1 );
    push @{ $lwpUserAgent->requests_redirectable }, 'POST';
    $lwpUserAgent->cookie_jar( { file => "/tmp/UserAgentCookies$$.txt" });
    # Apply basic authentication to the user agent if url, username and
    # password are supplied:
    if ( defined $url && defined $username && defined $password ) {
        my $loginType = ( defined $type ? $type : "basic" );
	if ( $loginType =~ /^basic$/ ) {
            my $realm = Sling::URL::url_to_realm( $url );
            $lwpUserAgent->credentials( $realm, 'Sling (Development)',
	                                $username => $password, );
	    if ( defined $log ) {
                Sling::Print::print_file_lock(
		    "Basic auth credentials for realm: \"$realm\" " .
		    "set for user: \"$username\"", $log );
            }
	    my $auth = new Sling::Authn( $url, \$lwpUserAgent );
	    my $success = $auth->basic_login( $log );
	    if ( ! $success ) {
	        die "Basic Auth log in for user \"$username\" at URL \"$url\" was unsuccessful\n";
	    }
        }
	elsif ( $loginType =~ /^form$/ ) {
	    my $auth = new Sling::Authn( $url, \$lwpUserAgent );
	    my $success = $auth->form_login( $username, $password, $log );
	    if ( ! $success ) {
	        die "Form log in for user \"$username\" at URL \"$url\" was unsuccessful\n";
	    }
	}
	else {
	    die "Unsupported auth type: \"$loginType\"\n"; 
	}
    }
    return \$lwpUserAgent;
}
#}}}

1;
