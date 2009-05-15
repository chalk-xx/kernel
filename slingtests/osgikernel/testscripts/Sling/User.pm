#!/usr/bin/perl

package Sling::User;

=head1 NAME

User - user related functionality for Sling implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST user methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::UserUtil;
use Sling::Util;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a User Agent.

=cut

sub new {
    my ( $class, $url, $lwpUserAgent ) = @_;
    die "url not defined!" unless defined $url;
    die "no lwp user agent provided!" unless defined $lwpUserAgent;
    my $response;
    my $user = { BaseURL => "$url",
                 LWP => $lwpUserAgent,
		 Message => "",
		 Response => \$response };
    bless( $user, $class );
    return $user;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $user, $message, $response ) = @_;
    $user->{ 'Message' } = $message;
    $user->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub add
sub add {
    my ( $user, $actOnUser, $actOnPass, $actOnEmail, $actOnFirst, $actOnLast, $log ) = @_;
    my $res = ${ $user->{ 'LWP' } }->request( Sling::Util::string_to_request(
        Sling::UserUtil::add_setup( $user->{ 'BaseURL' },
	    $actOnUser, $actOnPass, $actOnEmail, $actOnFirst, $actOnLast ), $user->{ 'LWP' } ) );
    my $success = Sling::UserUtil::add_eval( \$res );
    my $message = "User: \"$actOnUser\", email \"$actOnEmail\" ";
    $message .= ( $success ? "added!" : "was not added!" );
    $user->set_results( "$message", \$res );
    print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub add_from_file
sub add_from_file {
    my ( $user, $file, $forkId, $numberForks, $log ) = @_;
    my $count = 0;
    open ( FILE, $file );
    while ( <FILE> ) {
        if ( $forkId == ( $count++ % $numberForks ) ) {
            chomp;
	    $_ =~ /^(.*?),(.*?),(.*?),(.*?),(.*?)$/;
	    my $actOnUser = $1;
	    my $actOnPass = $2;
	    my $actOnEmail = $3;
	    my $actOnFirst = $4;
	    my $actOnLast = $5;
	    if ( defined $actOnUser && defined $actOnPass && defined $actOnEmail ) {
	        $user->add( $actOnUser, $actOnPass, $actOnEmail, $actOnFirst, $actOnLast, $log );
		Sling::Util::print_lock( $user->{ 'Message' } );
	    }
	}
    }
    close ( FILE ); 
    return 1;
}
#}}}

#{{{sub change_password
sub change_password {
    my ( $user, $actOnUser, $actOnPass, $newPass, $newPassConfirm, $log ) = @_;
    my $res = ${ $user->{ 'LWP' } }->request( Sling::Util::string_to_request(
        Sling::UserUtil::change_password_setup( $user->{ 'BaseURL' },
	    $actOnUser, $actOnPass, $newPass, $newPassConfirm ), $user->{ 'LWP' } ) );
    my $success = Sling::UserUtil::change_password_eval( \$res );
    my $message = "User: \"$actOnUser\" ";
    $message .= ( $success ? "password changed!" : "password not changed!" );
    $user->set_results( "$message", \$res );
    print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub delete
sub delete {
    my ( $user, $actOnUser, $log ) = @_;
    my $res = ${ $user->{ 'LWP' } }->request( Sling::Util::string_to_request(
        Sling::UserUtil::delete_setup( $user->{ 'BaseURL' }, $actOnUser ), $user->{ 'LWP' } ) );
    my $success = Sling::UserUtil::delete_eval( \$res );
    my $message = "User: \"$actOnUser\" ";
    $message .= ( $success ? "deleted!" : "was not deleted!" );
    $user->set_results( "$message", \$res );
    print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub exists
sub exists {
    my ( $user, $actOnUser, $log ) = @_;
    my $res = ${ $user->{ 'LWP' } }->request( Sling::Util::string_to_request(
                  Sling::UserUtil::exists_setup( $user->{ 'BaseURL' }, $actOnUser ), $user->{ 'LWP' } ) );
    my $success = Sling::UserUtil::exists_eval( \$res );
    my $message = "User \"$actOnUser\" ";
    $message .= ( $success ? "exists!" : "does not exist!" );
    $user->set_results( "$message", \$res );
    print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub me
sub me {
    my ( $user, $log ) = @_;
    my $res = ${ $user->{ 'LWP' } }->request( Sling::Util::string_to_request(
                  Sling::UserUtil::me_setup( $user->{ 'BaseURL' } ), $user->{ 'LWP' } ) );
    my $success = Sling::UserUtil::me_eval( \$res );
    my $message = ( $success ? $res->content : "Problem fetching details for current user" );
    $user->set_results( "$message", \$res );
    print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub view
sub view {
    my ( $user, $actOnUser, $log ) = @_;
    my $res = ${ $user->{ 'LWP' } }->request( Sling::Util::string_to_request(
                  Sling::UserUtil::exists_setup( $user->{ 'BaseURL' }, $actOnUser ), $user->{ 'LWP' } ) );
    my $success = Sling::UserUtil::exists_eval( \$res );
    my $message = ( $success ? $res->content : "Problem viewing user: \"$actOnUser\"" );
    $user->set_results( "$message", \$res );
    print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

1;
