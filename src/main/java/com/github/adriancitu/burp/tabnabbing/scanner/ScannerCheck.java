package com.github.adriancitu.burp.tabnabbing.scanner;

import burp.*;
import com.github.adriancitu.burp.tabnabbing.parser.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ScannerCheck implements IScannerCheck {


    private static final Logger LOGGER =
            Logger.getLogger(ScannerCheck.class.getName());

    private final IExtensionHelpers helpers;

    public ScannerCheck(final IExtensionHelpers helpers) {
        this.helpers = helpers;
    }

    /**
     * Creates an instance of {@link HTMLResponseReader}. It also
     * creates different {@link IByteReaderObserver}s that will
     * be attached to the {@link HTMLResponseReader} instance.
     *
     * @param httpToParse             the (HTTP) response to parse and check for problems.
     * @param isReffererHeaderPresent flag to indicate if the Referrer-Policy: no-referrer
     *                                header is present in the HTTP response.
     * @return an isntance of {@link HTMLResponseReader}
     */
    private HTMLResponseReader createResponseReader(
            byte[] httpToParse,
            boolean isReffererHeaderPresent) {
        HTMLResponseReader httpReader = new HTMLResponseReader(httpToParse);

        List<IByteReaderObserver> observers = new ArrayList<>();
        observers.add(new HTMLAnchorReaderObserver(isReffererHeaderPresent));
        observers.add(new JSWindowsOpenReaderObserver(isReffererHeaderPresent));
        httpReader.attachObservers(observers);

        return httpReader;
    }

    @Override
    public List<IScanIssue> doPassiveScan(
            final IHttpRequestResponse iHttpRequestResponse) {

        byte[] htmlResponse = iHttpRequestResponse.getResponse();

        HTMLResponseReader httpReader = createResponseReader(
                htmlResponse,
                isReferrerHeaderPresent(htmlResponse));

        try {

            final List<TabNabbingProblem> problems = httpReader.getProblems();

            return
                    problems.stream()
                            .map(
                                    problem ->
                                            new CustomScanIssue(
                                                    iHttpRequestResponse,
                                                    helpers.analyzeRequest(
                                                            iHttpRequestResponse).getUrl(),
                                                    problem.getIssueType(),
                                                    problem.getProblem()

                                            )).collect(Collectors.toList());

        } finally {
            try {
                httpReader.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    private boolean isReferrerHeaderPresent(byte[] htmlResponse) {
        return helpers.analyzeResponse(htmlResponse)
                .getHeaders()
                .stream()
                .anyMatch(header ->
                        "referrer-policy:no-referrer"
                                .equalsIgnoreCase(header
                                        .replaceAll(" ", "")));
    }

    @Override
    public List<IScanIssue> doActiveScan(
            final IHttpRequestResponse iHttpRequestResponse,
            final IScannerInsertionPoint iScannerInsertionPoint) {
        return Collections.emptyList();
    }

    @Override
    public int consolidateDuplicateIssues(
            final IScanIssue existingIssue,
            final IScanIssue newIssue) {
        if (existingIssue.getIssueDetail().equalsIgnoreCase(
                newIssue.getIssueDetail())) {
            return -1;
        } else {
            return 0;
        }
    }
}
