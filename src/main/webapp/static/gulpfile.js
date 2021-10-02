'use strict';

const gulp = require('gulp'),
    del = require('del'),
    concat = require('gulp-concat'),
    eslint = require('gulp-eslint'),
    uglify = require('gulp-uglify'),
    rename = require('gulp-rename'),
    banner = require('gulp-banner'),
    watch = require('gulp-watch'),
    pump = require('pump');

gulp.task('clean', function() {
    // Note sync(), see https://stackoverflow.com/a/34129950
    return del.sync(['build/*', 'release/*']);
});

gulp.task('concat-css', function() {
    const cssResources = [
        'vendor/fontawesome-free/css/all.css',
        'vendor/featherlight/featherlight.min.css',
        'css/nunito.css',
        'css/sb-admin-2.css'
    ];

    return gulp.src(cssResources)
        .pipe(concat('app.css'))
        .pipe(gulp.dest('build'));
});

gulp.task('watch-css', ['concat-css']);

gulp.task('minify-css', ['concat-css'], function() {
    return gulp.src('build/app.css')
        .pipe(rename({suffix: '.min'}))
        .pipe(gulp.dest('release'));
});

gulp.task('eslint-js', function() {
    return gulp.src('js/onyx/**/*.js')
        .pipe(eslint())
        .pipe(eslint.format())
        .pipe(eslint.failAfterError());
});

gulp.task('concat-js', ['eslint-js'], function() {
    const jsResources = [
        'vendor/jquery/jquery.min.js',
        'vendor/jquery.ui/jquery.ui.widget.js',
        'vendor/bootstrap/js/bootstrap.bundle.min.js',
        'vendor/jquery.fileupload/jquery.fileupload.js',
        'vendor/jquery.fileupload/jquery.fileupload-process.js',
        'vendor/jquery.fileupload/jquery.fileupload-validate.js',
        'vendor/jquery-easing/jquery.easing.min.js',
        'vendor/featherlight/featherlight.min.js',
        'js/sb-admin-2.js',
        'js/onyx/onyx.js',
        'js/onyx/app/app.js',
        'js/onyx/app/file.js',
        'js/onyx/app/directory.js',
        'js/onyx/app/featherlight-previewer.js'
    ];

      return gulp.src(jsResources)
          .pipe(concat('app.js'))
          .pipe(gulp.dest('build'));
});

gulp.task('watch-js', ['concat-js']);

gulp.task('minify-js', ['concat-js'], function(callback) {
    // uglify() wants pump()
    pump([
        gulp.src('build/app.js'),
        uglify(),
        rename({suffix: '.min'}),
        gulp.dest('release')
    ], callback);
});

gulp.task('banner', ['minify-css', 'minify-js'], function() {
    const comment =
        '/*\n' +
        ' * (c) Copyright 2021 Mark S. Kolich\n' +
        ' * All rights reserved.\n' +
        ' * https://mark.koli.ch\n' +
        ' */\n';

    return gulp.src(['release/*.css', 'release/*.js'])
        .pipe(banner(comment))
        .pipe(gulp.dest('release'));
});

// Watch & rebuild on any changes to js/html files
gulp.task('dev', ['concat-css', 'concat-js'], function() {
    gulp.watch('css/**/*.css', ['watch-css']);
    gulp.watch('js/**/*.js', ['watch-js']);
});

gulp.task('release', ['clean', 'concat-css', 'minify-css', 'concat-js', 'minify-js', 'banner']);
gulp.task('default', ['dev']);