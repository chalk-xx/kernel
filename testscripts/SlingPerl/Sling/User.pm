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
use Text::CSV;
use Sling::Print;
use Sling::Request;
use Sling::UserUtil;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a User Agent.

=cut

sub new {
    my ( $class, $authn, $verbose, $log ) = @_;
    die "no authn provided!" unless defined $authn;
    my $response;
    my $user = { BaseURL => $$authn->{ 'BaseURL' },
                 Authn => $authn,
		 Message => "",
		 Response => \$response,
		 Verbose => $verbose,
		 Log => $log };
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
    my ( $user, $actOnUser, $actOnPass, $properties ) = @_;
    my $res = Sling::Request::request( \$user,
        Sling::UserUtil::add_setup( $user->{ 'BaseURL' }, $actOnUser, $actOnPass, $properties ) );
    my $success = Sling::UserUtil::add_eval( $res );
    my $message = "User: \"$actOnUser\" ";
    $message .= ( $success ? "added!" : "was not added!" );
    $user->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub add_from_file
sub add_from_file {
    my ( $user, $file, $forkId, $numberForks ) = @_;
    my $csv = Text::CSV->new();
    my $count = 0;
    my $numberColumns = 0;
    my @column_headings;
    open ( FILE, $file );
    while ( <FILE> ) {
        if ( $count++ == 0 ) {
	    # Parse file column headings first to determine field names:
	    if ( $csv->parse( $_ ) ) {
	        @column_headings = $csv->fields();
		# First field must be site:
		if ( $column_headings[0] !~ /^[Uu][Ss][Ee][Rr]$/ ) {
		    die "First CSV column must be the user ID, column heading must be \"user\". Found: \"" . $column_headings[0] . "\".\n";
		}
		if ( $column_headings[1] !~ /^[Pp][Aa][Ss][Ss][Ww][Oo][Rr][Dd]$/ ) {
		    die "Second CSV column must be the user password, column heading must be \"password\". Found: \"" . $column_headings[0] . "\".\n";
		}
		$numberColumns = @column_headings;
	    }
	    else {
	        die "CSV broken, failed to parse line: " . $csv->error_input;
	    }
	}
        elsif ( $forkId == ( $count++ % $numberForks ) ) {
	    my @properties;
	    if ( $csv->parse( $_ ) ) {
	        my @columns = $csv->fields();
		my $columns_size = @columns;
		# Check row has same number of columns as there were column headings:
		if ( $columns_size != $numberColumns ) {
		    die "Found \"$columns_size\" columns. There should have been \"$numberColumns\".\nRow contents was: $_";
		}
		my $id = $columns[0];
		my $password = $columns[1];
		for ( my $i = 2; $i < $numberColumns ; $i++ ) {
                    my $value = $column_headings[ $i ] . "=" . $columns[ $i ];
		    push ( @properties, $value );
		}
                $user->add( $id, $password, \@properties );
		Sling::Print::print_result( $user );
	    }
	    else {
	        die "CSV broken, failed to parse line: " . $csv->error_input;
	    }
	}
    }
    close ( FILE ); 
    return 1;
}
#}}}

#{{{sub change_password
sub change_password {
    my ( $user, $actOnUser, $actOnPass, $newPass, $newPassConfirm ) = @_;
    my $res = Sling::Request::request( \$user,
        Sling::UserUtil::change_password_setup( $user->{ 'BaseURL' }, $actOnUser, $actOnPass, $newPass, $newPassConfirm ) );
    my $success = Sling::UserUtil::change_password_eval( $res );
    my $message = "User: \"$actOnUser\" ";
    $message .= ( $success ? "password changed!" : "password not changed!" );
    $user->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub delete
sub delete {
    my ( $user, $actOnUser ) = @_;
    my $res = Sling::Request::request( \$user,
        Sling::UserUtil::delete_setup( $user->{ 'BaseURL' }, $actOnUser ) );
    my $success = Sling::UserUtil::delete_eval( $res );
    my $message = "User: \"$actOnUser\" ";
    $message .= ( $success ? "deleted!" : "was not deleted!" );
    $user->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub exists
sub exists {
    my ( $user, $actOnUser ) = @_;
    my $res = Sling::Request::request( \$user,
        Sling::UserUtil::exists_setup( $user->{ 'BaseURL' }, $actOnUser ) );
    my $success = Sling::UserUtil::exists_eval( $res );
    my $message = "User \"$actOnUser\" ";
    $message .= ( $success ? "exists!" : "does not exist!" );
    $user->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub me
sub me {
    my ( $user ) = @_;
    my $res = Sling::Request::request( \$user,
        Sling::UserUtil::me_setup( $user->{ 'BaseURL' } ) );
    my $success = Sling::UserUtil::me_eval( $res );
    my $message = ( $success ? $$res->content : "Problem fetching details for current user" );
    $user->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub sites
sub sites {
    my ( $user ) = @_;
    my $res = Sling::Request::request( \$user,
        Sling::UserUtil::sites_setup( $user->{ 'BaseURL' } ) );
    my $success = Sling::UserUtil::sites_eval( $res );
    my $message = ( $success ? $$res->content : "Problem fetching details for current user" );
    $user->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub update
sub update {
    my ( $user, $actOnUser, $properties ) = @_;
    my $res = Sling::Request::request( \$user,
        Sling::UserUtil::update_setup( $user->{ 'BaseURL' }, $actOnUser, $properties ) );
    my $success = Sling::UserUtil::update_eval( $res );
    my $message = "User: \"$actOnUser\" ";
    $message .= ( $success ? "updated!" : "was not updated!" );
    $user->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub view
sub view {
    my ( $user, $actOnUser ) = @_;
    my $res = Sling::Request::request( \$user,
        Sling::UserUtil::exists_setup( $user->{ 'BaseURL' }, $actOnUser ) );
    my $success = Sling::UserUtil::exists_eval( $res );
    my $message = ( $success ? $$res->content : "Problem viewing user: \"$actOnUser\"" );
    $user->set_results( "$message", $res );
    return $success;
}
#}}}

1;
