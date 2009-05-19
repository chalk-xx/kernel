#!/usr/bin/perl

package Shell::User;

=head1 NAME

Shell::User - provides user related functionality for the Sling Shell.

=head1 ABSTRACT

An administrative terminal for Sling, allowing interactive administration of the Sling system.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use vars qw(@ISA @EXPORT);
use Sling::User;
use Term::ReadKey;
use Term::ANSIColor;
#}}}

@ISA = qw(Exporter);

require Exporter;

@EXPORT = qw(help_user_add smry_user_add help_user_exists smry_user_exists help_login smry_login);

#{{{sub run_user_add
sub run_user_add {
    my ( $term, $actOnUser, $config ) = @_;
    if ( ! defined $actOnUser ) {
        $actOnUser = $term->prompt("Please enter a user name: ");
    }
    if ( $actOnUser =~ /^\s*$/ ) {
        return 1;
    }
    print "\n";
    my $user = new Sling::User( $config->{ 'host' }, $config->{ 'lwp' } );
    if ( $user->exists( $actOnUser ) ) {
        print "User \"$actOnUser\" already exists!\n";
	return 1;
    }
    print colored ( "Please enter a password: ", 'bold underline' );
    ReadMode('noecho');
    my $actOnPass = ReadLine(0);
    ReadMode('restore');
    chomp( $actOnPass );
    print "\n";
    my $actOnEmail = $term->prompt("Please enter an email address: ");
    if ( $actOnEmail =~ /^\s*$/ ) {
        return 1;
    }
    my $user = new Sling::User( $config->{ 'host' }, $config->{ 'lwp' } );
    if ( ! $user->add( $actOnUser, $actOnPass, $actOnEmail ) ) {
        print ${ $user->{ 'Response' } }->status_line . "\n";
    }
    print $user->{ 'Message' } . "\n";
    return 1;
}
#}}}

#{{{sub help_user_add
sub help_user_add {
    return "Specify a username, password, and email to add a user to the system.";
}
#}}}

#{{{sub smry_user_add
sub smry_user_add {
    return "Add a new user to the system";
}
#}}}

#{{{sub run_user_exists
sub run_user_exists {
    my ( $term, $actOnUser, $config ) = @_;
    if ( ! defined $actOnUser ) {
        $actOnUser = $term->prompt("Please enter a user name: ");
    }
    if ( $actOnUser =~ /^\s*$/ ) {
        return 1;
    }
    my $user = new Sling::User( $config->{ 'host' }, $config->{ 'lwp' } );
    $user->exists( $actOnUser );
    print $user->{ 'Message' } . "\n";
    return 1;
}
#}}}

#{{{sub help_user_exists
sub help_user_exists {
    return "Specify a username to check whether that user exists in the system.";
}
#}}}

#{{{sub smry_user_exists
sub smry_user_exists {
    return "Check whether a user exists in the system";
}
#}}}

#{{{sub run_login
sub run_login {
    my ( $term, $username, $config ) = @_;
    if ( ! defined $username ) {
        $username = $term->prompt("Please enter a user name: ");
    }
    if ( $username =~ /^\s*$/ ) {
        return 1;
    }
    print colored ( "Please enter a password: ", 'underline' );
    ReadMode('noecho');
    my $password = ReadLine(0);
    ReadMode('restore');
    chomp( $password );
    print "\n";

    my $auth = new Sling::Auth( $config->{ 'host' }, $config->{ 'lwp' } );
    if ( $auth->form_login( $username, "$password" ) ) {
        $config->{ 'user' } = $username;
        print $auth->{ 'Message' } . "\n";
    }
    else {
        print $auth->{ 'Message' } . "\n";
        print ${ $auth->{ 'Response' } }->status_line . "\n";
        return 0;
    }
    return 1;
}
#}}}

#{{{sub help_login
sub help_login {
    return "Specify a username and password to log in the system.";
}
#}}}

#{{{sub smry_login
sub smry_login {
    return "Log in to the system";
}
#}}}

1;
