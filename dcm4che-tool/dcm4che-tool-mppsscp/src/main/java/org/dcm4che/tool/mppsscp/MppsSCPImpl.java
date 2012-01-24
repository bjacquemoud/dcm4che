/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2012
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4che.tool.mppsscp;

import java.io.File;
import java.io.IOException;

import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.UID;
import org.dcm4che.io.DicomInputStream;
import org.dcm4che.io.DicomOutputStream;
import org.dcm4che.net.Association;
import org.dcm4che.net.Status;
import org.dcm4che.net.service.BasicMppsSCP;
import org.dcm4che.net.service.DicomServiceException;
import org.dcm4che.util.SafeClose;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 *
 */
class MppsSCPImpl extends BasicMppsSCP {

    private final MppsSCP mppsSCP;

    MppsSCPImpl(MppsSCP mppsSCP) {
        this.mppsSCP = mppsSCP;
    }

    @Override
    protected Attributes create(Association as, Attributes rq,
            Attributes rqAttrs, Attributes rsp) throws DicomServiceException {
        File storeDir = mppsSCP.getStorageDirectory();
        if (storeDir == null)
            return null;
        String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
        File file = new File(storeDir, iuid);
        if (file.exists())
            throw new DicomServiceException(Status.DuplicateSOPinstance).
                setUID(Tag.AffectedSOPInstanceUID, iuid);
        DicomOutputStream out = null;
        LOG.info("{}: M-WRITE {}", as, file);
        try {
            out = new DicomOutputStream(file);
            out.writeDataset(
                    Attributes.createFileMetaInformation(iuid,
                            UID.ModalityPerformedProcedureStepNotificationSOPClass,
                            UID.ExplicitVRLittleEndian),
                    rqAttrs);
        } catch (IOException e) {
            LOG.warn(as + ": Failed to store mpps:", e);
            throw new DicomServiceException(Status.ProcessingFailure, e);
        } finally {
            SafeClose.close(out);
        }
        return null;
    }

    @Override
    protected Attributes set(Association as, Attributes rq, Attributes rqAttrs,
            Attributes rsp) throws DicomServiceException {
        File storeDir = mppsSCP.getStorageDirectory();
        if (storeDir == null)
            return null;
        String iuid = rq.getString(Tag.RequestedSOPInstanceUID);
        File file = new File(storeDir, iuid);
        if (!file.exists())
            throw new DicomServiceException(Status.NoSuchObjectInstance).
                setUID(Tag.AffectedSOPInstanceUID, iuid);
        LOG.info("{}: M-UPDATE {}", as, file);
        Attributes mpps;
        DicomInputStream in = null;
        try {
            in = new DicomInputStream(file);
            mpps = in.readDataset(-1, -1);
        } catch (IOException e) {
            LOG.warn(as + ": Failed to read mpps:", e);
            throw new DicomServiceException(Status.ProcessingFailure, e);
        } finally {
            SafeClose.close(in);
        }
        mpps.addAll(rqAttrs);
        DicomOutputStream out = null;
        try {
            out = new DicomOutputStream(file);
            out.writeDataset(
                    Attributes.createFileMetaInformation(iuid,
                            UID.ModalityPerformedProcedureStepNotificationSOPClass,
                            UID.ExplicitVRLittleEndian),
                            mpps);
        } catch (IOException e) {
            LOG.warn(as + ": Failed to update mpps:", e);
            throw new DicomServiceException(Status.ProcessingFailure, e);
        } finally {
            SafeClose.close(out);
        }
        return null;
    }

}
