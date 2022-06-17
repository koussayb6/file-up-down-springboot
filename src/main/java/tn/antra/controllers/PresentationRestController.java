package tn.antra.controllers;

import static java.nio.file.Paths.get;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import static java.nio.file.Files.copy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import tn.antra.entities.Presentation;
import tn.antra.repositories.PresentationRepository;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/presentations")
public class PresentationRestController {

	public static final String DIRECTORY = "C:\\Users\\kouss\\Documents\\workspace-spring-tool-suite-4-4.12.0.RELEASE\\uploads\\";
	public static final String IMG_DIRECTORY = "C:\\Users\\kouss\\Documents\\workspace-spring-tool-suite-4-4.12.0.RELEASE\\presentations\\src\\main\\resources\\image\\";

	List<String> imageExtentions = new ArrayList<>(Arrays.asList("jpg", "jpeg"));
	List<String> fileExtentions = new ArrayList<>(Arrays.asList("pdf", "ppt", "pptx"));

	@Autowired
	PresentationRepository presentationRepository;

	@PostMapping
	public ResponseEntity<?> addPresentation(@RequestParam("file") MultipartFile multipartFile,
			@RequestParam("preview") MultipartFile image, @RequestParam("title") String title,
			@RequestParam("description") String description) throws IOException {

		String fileName = UUID.randomUUID().toString() + multipartFile.getOriginalFilename();
		String imageName = UUID.randomUUID().toString() + image.getOriginalFilename();
		String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
		String imageExtension = imageName.substring(imageName.lastIndexOf(".") + 1);
		if (!(this.fileExtentions.contains(extension) && this.imageExtentions.contains(imageExtension)))
			return ResponseEntity.badRequest().body("file type invalid");

		Presentation presentation = new Presentation();
		presentation.setTitle(title);
		presentation.setDescription(description);
		presentation.setFileName(fileName);
		presentation.setPreviewImage(imageName);
		Presentation pres = presentationRepository.save(presentation);
		try {
			Path fileStorage = get(DIRECTORY, fileName).toAbsolutePath().normalize();
            copy(multipartFile.getInputStream(), fileStorage);
            Path imageStorage = get(DIRECTORY, imageName).toAbsolutePath().normalize();
            copy(image.getInputStream(), imageStorage);
			/*multipartFile.transferTo(new File(DIRECTORY + fileName));
			image.transferTo(new File(DIRECTORY + imageName));*/

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.toString());
		}

		pres.setPicByte(image.getBytes());
		return ResponseEntity.ok().body(pres);

	}

	@GetMapping
	public ResponseEntity<List<Presentation>> getAll() throws IOException {
		List<Presentation> list = presentationRepository.findAllByOrderByIdDesc();
		for (Presentation pres : list) {
			Path filePath = get(DIRECTORY).toAbsolutePath().normalize().resolve(pres.getPreviewImage());

			byte[] bytes = Files.readAllBytes(filePath);
			pres.setPicByte(bytes);
		}
		return ResponseEntity.ok().body(list);
	}

	@GetMapping("download/{filename}")
	public ResponseEntity<Resource> downloadFile(@PathVariable("filename") String filename) throws IOException {
		Path filePath = get(DIRECTORY).toAbsolutePath().normalize().resolve(filename);
		if (!Files.exists(filePath)) {
			throw new FileNotFoundException(filename + " was not found on the server");
		}
		Resource resource = new UrlResource(filePath.toUri());

		return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(filePath))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
	}

	@RequestMapping(value = "/{filename}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	public ResponseEntity<InputStreamResource> getImage(@PathVariable("filename") String filename) throws IOException {

		ClassPathResource imgFile = new ClassPathResource("image/" + filename);

		return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
				.body(new InputStreamResource(imgFile.getInputStream()));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Presentation> delete(@PathVariable("id") Long id) throws IOException {
		Presentation pres = presentationRepository.findById(id).orElse(null);

		Path filePath = get(DIRECTORY).toAbsolutePath().normalize().resolve(pres.getFileName());
		Path imgPath = get(DIRECTORY).toAbsolutePath().normalize().resolve(pres.getPreviewImage());
		if (!Files.exists(filePath)) {
			throw new FileNotFoundException(pres.getFileName() + " was not found on the server");
		}
		if (!Files.exists(imgPath)) {
			throw new FileNotFoundException(pres.getPreviewImage() + " was not found on the server");
		}
		filePath.toFile().delete();
		imgPath.toFile().delete();
		presentationRepository.deleteById(id);
		return ResponseEntity.ok().body(pres);

	}

	@PutMapping
	public ResponseEntity<?> updatePresentation(@RequestParam(required = false) MultipartFile file,
			@RequestParam(required = false) MultipartFile preview, @RequestParam("title") String title,
			@RequestParam("description") String description, @RequestParam("id") Long id) throws IOException {

		Presentation pres = presentationRepository.findById(id).orElse(null);
		pres.setTitle(title);
		pres.setDescription(description);
		if (file != null && preview != null) {

			String fileName = UUID.randomUUID().toString() + file.getOriginalFilename();
			String imageName = UUID.randomUUID().toString() + preview.getOriginalFilename();
			String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
			String imageExtension = imageName.substring(imageName.lastIndexOf(".") + 1);
			if (!(this.fileExtentions.contains(extension) && this.imageExtentions.contains(imageExtension)))
				return ResponseEntity.badRequest().body("file type invalid");
			Path filePath = get(DIRECTORY).toAbsolutePath().normalize().resolve(pres.getFileName());
			Path imgPath = get(DIRECTORY).toAbsolutePath().normalize().resolve(pres.getPreviewImage());
			filePath.toFile().delete();
			imgPath.toFile().delete();
			pres.setFileName(fileName);
			pres.setPreviewImage(imageName);
			try {
				Path fileStorage = get(DIRECTORY).toAbsolutePath().normalize().resolve(fileName);
				Path imgStorage = get(DIRECTORY).toAbsolutePath().normalize().resolve(imageName);
	            copy(file.getInputStream(), fileStorage);
	            copy(preview.getInputStream(), imgStorage);

			} catch (Exception e) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.toString());
			}
		}
		Presentation presentation = presentationRepository.save(pres);

		if (preview != null)
			presentation.setPicByte(preview.getBytes());
		else {
			Path filePath = get(DIRECTORY).toAbsolutePath().normalize().resolve(pres.getPreviewImage());

			byte[] bytes = Files.readAllBytes(filePath);
			presentation.setPicByte(bytes);
		}
		return ResponseEntity.ok().body(presentation);

	}

}