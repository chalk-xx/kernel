#!/usr/bin/perl

package Shell::Terminal;

=head1 NAME

An administrative terminal for Sling, allowing interactive administration of the Sling system.

=head1 ABSTRACT

An administrative terminal for Sling, allowing interactive administration of the Sling system.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use base qw(Term::Shell);
use Shell::Search;
# use Shell::Site;
use Shell::User;
#}}}

# Obtain reference to LWP user agent object:
my $lwpUserAgent; # TODO fixup = Sling::UserAgent::get_user_agent;

my %config = ( prompt => 'sling',
               user => 'anon',
	       host => 'disconnected',
	       lwp => $lwpUserAgent );

#{{{sub alias_exit
# alias quit to exit so the obvious ways of exiting the terminal are covered.
sub alias_exit() {
    return ( "quit" );
}
#}}}

#{{{sub prompt_str
sub prompt_str {
    if ( $config{ 'host' } =~ "disconnected" ) {
        my $sh = Term::Shell->new();
        Shell::Terminal::run_host( $sh );
    }
    return $config{ 'host' } . " - " . $config{ 'user' } . "> ";
}
#}}}

#{{{host functions

#{{{sub run_host
sub run_host {
    my ( $term, $host ) = @_;
    if ( ! defined $host && ( $config{ 'host' } !~ /^disconnected$/ ) ) {
        print "You are currently using host: \"" . $config{ 'host' } . "\"";
	return 1;
    }
    elsif ( ! defined $host ) {
        $host = $term->prompt("Please enter a host (e.g. localhost:8080,etc): ");
    }
    if ( $host =~ /^\s*$/ ) {
        return 1;
    }
    if ( $host !~ /^http/ ) {
        my $http;
	while ( ! defined $http ) {
            my $ssl = $term->prompt("Does the host use SSL (https://)? (Y/n): ","Y");
	    if ( $ssl =~ /^[Yy]/ ) { $http = "https://" };
	    if ( $ssl =~ /^[Nn]/ ) { $http = "http://" };
	}
	$host = "$http$host";
    }
    $config{ 'host' } = $host; 
    return 1;
}
#}}}

#{{{sub help_host
sub help_host {
    return "With no argument the host command returns the host currently being \n".
           "used by the shell, unless no host is set in which case a new host will\n".
	   "be prompted for and the host set to the value supplied. If an argument\n".
	   "is given the host will be set to that value. The host command will also\n".
	   "check for ssl settings prior to changing the host.";
}
#}}}

#{{{sub smry_host
sub smry_host {
    return "Check or set currently used host";
}
#}}}

#}}}

#{{{search functions

#{{{sub run_search
sub run_search {
    my ( $term, $searchTerm ) = @_;
    Shell::Search::run_search( $term, $searchTerm, \%config );
    return 1;
}
#}}}

#}}}

#{{{user functions

#{{{sub run_user_add
sub run_user_add {
    my ( $term, $actOnUser ) = @_;
    Shell::User::run_user_add( $term, $actOnUser, \%config );
    return 1;
}
#}}}

#{{{sub run_user_exists
sub run_user_exists {
    my ( $term, $actOnUser ) = @_;
    Shell::User::run_user_exists( $term, $actOnUser, \%config );
    return 1;
}
#}}}

#{{{sub run_user_whoami
sub run_user_whoami {
    my ( $term ) = @_;
    Shell::User::run_user_whoami( $term, \%config );
    return 1;
}
#}}}

#{{{sub run_login
sub run_login {
    my ( $term, $username ) = @_;
    Shell::User::run_login( $term, $username, \%config );
    return 1;
}
#}}}

#{{{sub run_logout
sub run_logout {
    my ( $term ) = @_;
    Shell::User::run_logout( $term, \%config );
    return 1;
}
#}}}
#}}}

#{{{site functions

#{{{sub run_site_add
# sub run_site_add {
    # my ( $term, $id ) = @_;
    # Shell::Site::run_site_add( $term, $id, \%config );
    # return 1;
# }
#}}}

#{{{sub run_site_add_owner
# sub run_site_add_owner {
    # my ( $term, $id ) = @_;
    # Shell::Site::run_site_add_owner( $term, $id, \%config );
    # return 1;
# }
#}}}

#{{{sub run_site_exists
# sub run_site_exists {
    # my ( $term, $id ) = @_;
    # Shell::Site::run_site_exists( $term, $id, \%config );
    # return 1;
# }
#}}}

#{{{sub run_site_owners
# sub run_site_owners {
    # my ( $term, $id ) = @_;
    # Shell::Site::run_site_owners( $term, $id, \%config );
    # return 1;
# }
#}}}

#}}}

1;
