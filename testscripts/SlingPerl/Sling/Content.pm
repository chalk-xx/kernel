#!/usr/bin/perl

package Sling::Content;

=head1 NAME

Content - content related functionality for Sling implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST content methods

=head2 Methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::ContentUtil;
use Sling::Print;
use Sling::Request;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Content object.

=cut

sub new {
    my ( $class, $authn, $verbose, $log ) = @_;
    die "no authn provided!" unless defined $authn;
    my $response;
    my $content = { BaseURL => $$authn->{ 'BaseURL' },
                    Authn => $authn,
		    Message => "",
		    Response => \$response,
		    Verbose => $verbose,
		    Log => $log };
    bless( $content, $class );
    return $content;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $content, $message, $response ) = @_;
    $content->{ 'Message' } = $message;
    $content->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub add
sub add {
    my ( $content, $remoteDest, $properties ) = @_;
    my $res = Sling::Request::request( \$content,
        Sling::ContentUtil::add_setup( $content->{ 'BaseURL' }, $remoteDest, $properties ) );
    my $success = Sling::ContentUtil::add_eval( $res );
    my $message = "Content addition to \"$remoteDest\" ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    $content->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub copy
sub copy {
    my ( $content, $remoteSrc, $remoteDest, $replace ) = @_;
    my $res = Sling::Request::request( \$content,
        Sling::ContentUtil::copy_setup( $content->{ 'BaseURL' }, $remoteSrc, $remoteDest, $replace ) );
    my $success = Sling::ContentUtil::copy_eval( $res );
    my $message = "Content copy from \"$remoteSrc\" to \"$remoteDest\" ";
    $message .= ( $success ? "completed!" : "did not complete successfully!" );
    $content->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub delete
sub delete {
    my ( $content, $remoteDest ) = @_;
    my $res = Sling::Request::request( \$content,
        Sling::ContentUtil::delete_setup( $content->{ 'BaseURL' }, $remoteDest ) );
    my $success = Sling::ContentUtil::delete_eval( $res );
    my $message = "Content \"$remoteDest\" ";
    $message .= ( $success ? "deleted!" : "was not deleted!" );
    $content->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub exists
sub exists {
    my ( $content, $remoteDest ) = @_;
    my $res = Sling::Request::request( \$content,
        Sling::ContentUtil::exists_setup( $content->{ 'BaseURL' }, $remoteDest ) );
    my $success = Sling::ContentUtil::exists_eval( $res );
    my $message = "Content \"$remoteDest\" ";
    $message .= ( $success ? "exists!" : "does not exist!" );
    $content->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub move
sub move {
    my ( $content, $remoteSrc, $remoteDest, $replace ) = @_;
    my $res = Sling::Request::request( \$content,
        Sling::ContentUtil::move_setup( $content->{ 'BaseURL' }, $remoteSrc, $remoteDest, $replace ) );
    my $success = Sling::ContentUtil::move_eval( $res );
    my $message = "Content move from \"$remoteSrc\" to \"$remoteDest\" ";
    $message .= ( $success ? "completed!" : "did not complete successfully!" );
    $content->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub upload_file
sub upload_file {
    my ( $content, $localPath, $remotePath, $filename ) = @_;
    my $res = Sling::Request::request( \$content,
        Sling::ContentUtil::upload_file_setup( $content->{ 'BaseURL' }, $localPath, $remotePath, $filename ) );
    my $success = Sling::ContentUtil::upload_file_eval( $res );
    my $basename = $localPath;
    $basename =~ s/^(.*\/)([^\/]*)$/$2/;
    my $remoteDest = $remotePath . ( $filename !~ /^$/ ? "/$filename" : "/$basename" );
    my $message = "Content: \"$localPath\" upload to \"$remoteDest\" ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    $content->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub upload_from_file
sub upload_from_file {
    my ( $content, $file, $forkId, $numberForks ) = @_;
    my $count = 0;
    open ( FILE, $file );
    while ( <FILE> ) {
        if ( $forkId == ( $count++ % $numberForks ) ) {
            chomp;
	    $_ =~ /^(.*?),(.*?)$/;
	    my $localPath = $1;
	    my $remotePath = $2;
	    if ( defined $localPath && defined $remotePath ) {
	        $content->upload_file( $localPath, $remotePath, "" );
                Sling::Print::print_result( $content );
	    }
	    else {
	        print "ERROR: Problem parsing content to add: \"$_\"\n";
	    }
	}
    }
    close ( FILE ); 
    return 1;
}
#}}}

#{{{sub view
sub view {
    my ( $content, $remoteDest ) = @_;
    my $res = Sling::Request::request( \$content,
        Sling::ContentUtil::exists_setup( $content->{ 'BaseURL' }, $remoteDest ) );
    my $success = Sling::ContentUtil::exists_eval( $res );
    my $message = ( $success ? $$res->content : "Problem viewing content: \"$remoteDest\"" );
    $content->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub view_file
sub view_file {
    my ( $content, $remoteDest ) = @_;
    my $res  = Sling::Request::request( \$content, "get $content->{ 'BaseURL' }/$remoteDest");
    my $success = Sling::ContentUtil::exists_eval( $res );
    my $message = ( $success ? ${$res}->content : "Problem viewing content: \"$remoteDest\"" );
    $content->set_results( "$message", $res );
    return $success;
}
#}}}

1;
