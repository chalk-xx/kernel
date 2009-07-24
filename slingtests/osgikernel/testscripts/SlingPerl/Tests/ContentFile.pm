#!/usr/bin/perl

package Tests::ContentFile;

#{{{imports

use warnings;
use strict;
use Carp;
use lib qw ( .. );
use Sling::Content;
use Test::More;
use version; our $VERSION = qv('0.0.1');
use FileHandle;
use Digest::MD5 'md5_hex';
use Data::Dumper;

#}}}

#{{{sub run_regression_test
sub run_regression_test {
    my ( $authn, $verbose, $log ) = @_;
    my $test_file_path = 'data/test_tiny.jpg';
    my $test_file_name = 'test.jpg';
    my $test_file_dest = "content_file_test_blob_$$";

    # Sling content object:
    my $content = new Sling::Content( $authn, $verbose, $log );

    # Run tests:
    ok( defined $content,
        'Content File Test: Sling Content Object successfully created.' );
    ok(
        $content->upload_file(
            $test_file_path, $test_file_dest, $test_file_name
        ),
"Content File Test: Content \"$test_file_name\" uploaded successfully to $test_file_dest."
    );
    ok( $content->exists($test_file_dest),
        "Content File Test: Content \"$test_file_dest\" exists." );

    # is it the same?
    my $file = FileHandle->new( $test_file_path, q{r} );
    my $data;
    if ( defined $file ) {
        while (<$file>) {
            $data .= $_;
        }
        undef $file;    # automatically closes the file
    }
    my $blob_hash = 0;
    if ($data) {
        $blob_hash = md5_hex($data);
    }
    $content->view_file("$test_file_dest/$test_file_name");
    my $fetched_hash = 1;
    my $message      = $content->{'Message'};

    # carp 'content is ' , Dumper $content;
    $fetched_hash = md5_hex( $content->{'Message'} );

    # carp "hashes are $fetched_hash ==? $blob_hash";

    ok( $blob_hash eq $fetched_hash,
        "Content File Test: \"$fetched_hash\" == \"$blob_hash\" means file in matches file out."
    );

    ok( $content->delete($test_file_dest),
        "Content File Test: Content \"$test_file_dest\" deleted successfully." );
    ok( !$content->exists($test_file_dest),
        "Content File Test: Content \"$test_file_dest\" should no longer exist." );

    return;
}

#}}}

1;

__END__

