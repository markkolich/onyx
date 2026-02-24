'use strict';

const gulp = require('gulp'),
    del = require('del'),
    concat = require('gulp-concat'),
    eslint = require('gulp-eslint'),
    uglify = require('gulp-uglify'),
    cleanCSS = require('gulp-clean-css'),
    rename = require('gulp-rename'),
    banner = require('gulp-banner'),
    pump = require('pump');

function clean() {
    return del(['build/*', 'release/*']);
}

function concatCss() {
    const cssResources = [
        'vendor/fontawesome-free/css/all.css',
        'vendor/magnific-popup/magnific-popup.min.css',
        'css/magnific-popup.css',
        'css/nunito.css',
        'css/sb-admin-2.css',
        'css/sb-admin-2-dark.css'
    ];

    return gulp.src(cssResources)
        .pipe(concat('app.css'))
        .pipe(gulp.dest('build'));
}

function minifyCss() {
    return gulp.src('build/app.css')
        .pipe(cleanCSS())
        .pipe(rename({suffix: '.min'}))
        .pipe(gulp.dest('release'));
}

function eslintJs() {
    return gulp.src('js/onyx/**/*.js')
        .pipe(eslint())
        .pipe(eslint.format())
        .pipe(eslint.failAfterError());
}

function concatJs() {
    const jsResources = [
        'vendor/jquery/jquery.min.js',
        'vendor/jquery.ui/jquery.ui.widget.js',
        'vendor/bootstrap/js/bootstrap.bundle.min.js',
        'vendor/jquery.fileupload/jquery.fileupload.js',
        'vendor/jquery.fileupload/jquery.fileupload-process.js',
        'vendor/jquery.fileupload/jquery.fileupload-validate.js',
        'vendor/jquery.copy-to-clipboard/jquery.copy-to-clipboard.js',
        'vendor/jquery-easing/jquery.easing.min.js',
        'vendor/magnific-popup/jquery.magnific-popup.min.js',
        'vendor/keypress/keypress-2.1.5.min.js',
        'vendor/underscore/underscore-1.9.2.min.js',
        'js/sb-admin-2.js',
        'js/onyx/onyx.js',
        'js/onyx/app/app.js',
        'js/onyx/app/file.js',
        'js/onyx/app/directory.js',
        'js/onyx/app/shortlink.js',
        'js/onyx/app/previewer.js',
        'js/onyx/app/webauthn.js',
        'js/onyx/app/widgets/dark-mode.js'
    ];

    return gulp.src(jsResources)
        .pipe(concat('app.js'))
        .pipe(gulp.dest('build'));
}

function minifyJs(callback) {
    // uglify() wants pump()
    pump([
        gulp.src('build/app.js'),
        uglify(),
        rename({suffix: '.min'}),
        gulp.dest('release')
    ], callback);
}

function addBanner() {
    const comment =
        '/*\n' +
        ' * (c) Copyright 2026 Mark S. Kolich\n' +
        ' * All rights reserved.\n' +
        ' * https://mark.koli.ch\n' +
        ' */\n';

    return gulp.src(['release/*.css', 'release/*.js'])
        .pipe(banner(comment))
        .pipe(gulp.dest('release'));
}

// Watch & rebuild on any changes to js/html files
function dev() {
    gulp.watch('css/**/*.css', concatCss);
    gulp.watch('js/**/*.js', gulp.series(eslintJs, concatJs));
}

const buildCss = gulp.series(concatCss, minifyCss);
const buildJs = gulp.series(eslintJs, concatJs, minifyJs);
const release = gulp.series(clean, gulp.parallel(buildCss, buildJs), addBanner);

gulp.task('clean', clean);
gulp.task('concat-css', concatCss);
gulp.task('minify-css', gulp.series(concatCss, minifyCss));
gulp.task('eslint-js', eslintJs);
gulp.task('concat-js', gulp.series(eslintJs, concatJs));
gulp.task('minify-js', gulp.series(eslintJs, concatJs, minifyJs));
gulp.task('banner', gulp.series(buildCss, buildJs, addBanner));
gulp.task('dev', gulp.series(gulp.parallel(concatCss, gulp.series(eslintJs, concatJs)), dev));
gulp.task('release', release);
gulp.task('default', gulp.task('dev'));
