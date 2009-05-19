#!/usr/bin/perl

package Sling::Print;

=head1 NAME

Print - useful utility functions for general print to screeen and print to file
functionality.

=head1 ABSTRACT

Utility library providing useful utility functions for general Print
functionality.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Fcntl ':flock';
#}}}

#{{{sub print_file_lock

=pod

=head2 print_file_lock

Prints out a specified message to a specified file with locking in an attempt
to prevent competing threads or forks from stepping on each others toes when
writing to the file.

=cut

sub print_file_lock {
    my ( $message, $file ) = @_;
    if ( open( FILE, ">>$file" ) ) {
        flock( FILE, LOCK_EX );
        print FILE $message . "\n";
        flock( FILE, LOCK_UN );
        close( FILE );
    }
    else {
        die "Could not open file: $file";
    }
    return 1;
}
#}}}

#{{{sub print_lock

=pod

=head2 print_lock

Prints out a specified message with locking in an attempt to prevent competing
threads or forks from stepping on each others toes when printing to stdout.

=cut

sub print_lock {
    my ( $message ) = @_;
    if ( open( LOCK, ">>/tmp/RestLock$$.txt" ) ) {
        flock( LOCK, LOCK_EX );
        print $message . "\n";
        flock( LOCK, LOCK_UN );
        close( LOCK );
        unlink( "/tmp/RestLock$$.txt" );
    }
    else {
        die "Could not open lock file: /tmp/RestLock$$.txt";
    }
    return 1;
}
#}}}

1;
