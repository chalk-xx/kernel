#!/usr/bin/perl

package Sling::DAV;

=head1 NAME

DAV - dav related functionality for Sling

=head1 ABSTRACT

Library to abstract sling DAV functionality.

=cut

#{{{imports
use strict;
use lib qw ( .. );
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a DAV Agent class.

=cut

sub new {
    my ( $class, $davConn, $url ) = @_;
    # Create and return the user object:
    my $dav = { DavConn  => $davConn,
                 BaseURL  => "$url" };
    bless( $dav, $class );
    return $dav;
}
#}}}

#{{{sub upload

=pod

=head2 upload

Upload content to the sling system over webdav.

=cut

sub upload {
    my ( $self, $localPath, $remotePath ) = @_;
    my $baseURL= $self->{ 'BaseURL' };

    my $remoteDir = $remotePath;
    $remoteDir =~ s%[^/]*$%%;

    open (TMP, '>>/tmp/DAVFile.txt');
    if ( ${ $self->{ 'DavConn' } }->open( -url=> "$baseURL$remoteDir" ) ) {
        print TMP "INFO: opened $baseURL$remoteDir\n";
        if ( ! ${ $self->{ 'DavConn' } }->put(
	    -local => "$localPath",
	    -url => "$baseURL/$remotePath" ) ) {
            print TMP "ERROR: upload of $localPath to $remotePath failed.\n";
	    print TMP ${ $self->{ 'DavConn' } }->message;
	    return 0;
	}
    }
    else {
        print TMP "ERROR: Could not open $baseURL$remoteDir.\n";
	print TMP ${ $self->{ 'DavConn' } }->message;
	return 0;
    }
    print TMP "INFO: File " . $localPath . " successfully uploaded to" . $remotePath . "\n";
    return 1;
}
#}}}

#{{{sub upload_from_file
sub upload_from_file {
    my ( $dav, $file, $forkId, $numberForks, $path ) = @_;
    $forkId = 0 unless defined $forkId;
    $numberForks = 1 unless defined $numberForks;
    my $count = 0;
    open ( FILE, $file );
    while ( <FILE> ) {
        if ( $forkId == ( $count++ % $numberForks ) ) {
            chomp;
	    $_ =~ /^(.*?),(.*?)$/;
	    my $localPath = $1;
	    my $remotePath = $2;
	    if ( $localPath !~ /^$/ && $remotePath !~ /^$/ ) {
                my $type = ( -f $localPath ? "file" : ( -d $localPath ? "directory" : "" ) );
		die "ERROR: Unsupported Local path type for \"$localPath\"" if ( $type =~ /^$/ );
                print "Uploading $type $localPath to $remotePath: ";
                print ( $dav->upload( $localPath, $remotePath ) ? "Done!\n" : "Failed!\n" );
	    }
	}
    }
    close ( FILE ); 
    return 1;
}
#}}}

1;
